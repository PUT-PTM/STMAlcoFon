#include "stm32f4xx_conf.h"
#include "stm32f4xx_gpio.h"
#include "stm32f4xx_rcc.h"
#include "stm32f4xx_tim.h"
#include "stm32f4xx_exti.h"
#include "misc.h"
#include "stm32f4xx_adc.h"
#include "stm32f4xx_syscfg.h"
#include "stm32f4xx_usart.h"

//zmienne globalne
uint16_t ADC_Result;
uint8_t czy_wysylac=0;//domyslnie wylaczony

void usart_wyslijznak(uint8_t znak)
{
	while (!(USART3->SR & USART_SR_TXE));
	USART_SendData(USART3, znak);
}

void usart_wyslijliczbe(uint16_t x)
{
	char liczba[8]={' ',' ',' ',' '};
	int i = 0;
	do
	{
		liczba[i++] = (char)(x % 10) + 48;
		x /= 10;
	} while(x);
	for(i=3;i>=0;i--)
	{
		while (!(USART3->SR & USART_SR_TXE));
		USART_SendData(USART3, liczba[i]);
	}
}

uint8_t usart_odbierzznak()
{
	while(USART_GetFlagStatus(USART3, USART_FLAG_RXNE)==RESET);
	return USART_ReceiveData(USART3);
}

void USART3_IRQHandler(void)
{
	if(USART_GetITStatus(USART3,USART_IT_RXNE)!=RESET)
	{
		char temp=USART3->DR;//wczytanie do temp odebranego znaku
		if(temp=='C')//wlaczenie trybu ciaglego
		{
			czy_wysylac=-1;
		}
		if(temp=='S')//wylaczenie trybu ciaglego
		{
			czy_wysylac=0;
		}
		if(temp=='P')//pomiar lub kalibracja - wysylanie danych przez 5 sekund
		{
			czy_wysylac=20;//20 próbek wysylanych 4/s to daje 5 sekund
		}
		if(temp=='1')//pomiar lub kalibracja - wysylanie danych przez 5 sekund
		{
			czy_wysylac=1;//pojedyncza próbka
		}

	}
}

void przycisk()
{
	GPIO_InitTypeDef  konfiguracja_przycisku;
	konfiguracja_przycisku.GPIO_Pin = GPIO_Pin_0;
	konfiguracja_przycisku.GPIO_Mode = GPIO_Mode_IN;
	konfiguracja_przycisku.GPIO_OType = GPIO_OType_PP;
	konfiguracja_przycisku.GPIO_Speed = GPIO_Speed_100MHz;
	konfiguracja_przycisku.GPIO_PuPd = GPIO_PuPd_NOPULL;
	GPIO_Init(GPIOA, &konfiguracja_przycisku);
}

void diody()
{
	RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOD, ENABLE);
	//12 - zielona
	//13 - pomaranczowa
	//14 - czerwona
	//15 - niebieska
	GPIO_InitTypeDef  konfiguracja_diod;
	konfiguracja_diod.GPIO_Pin = GPIO_Pin_12 | GPIO_Pin_13| GPIO_Pin_14| GPIO_Pin_15;
	konfiguracja_diod.GPIO_Mode = GPIO_Mode_OUT;
	konfiguracja_diod.GPIO_OType = GPIO_OType_PP;
	konfiguracja_diod.GPIO_Speed = GPIO_Speed_100MHz;
	konfiguracja_diod.GPIO_PuPd = GPIO_PuPd_NOPULL;
	GPIO_Init(GPIOD, &konfiguracja_diod);
}

void timer5()
{
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_TIM5, ENABLE );
	TIM_TimeBaseInitTypeDef konfiguracja_timer;
	konfiguracja_timer.TIM_Period=2500;
	konfiguracja_timer.TIM_Prescaler=8399;
	konfiguracja_timer.TIM_ClockDivision=TIM_CKD_DIV1;
	konfiguracja_timer.TIM_CounterMode=TIM_CounterMode_Up;
	TIM_TimeBaseInit(TIM5,&konfiguracja_timer);
	TIM_Cmd(TIM5, ENABLE );
}

void przerwanie_timer5()
{
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_1);
	NVIC_InitTypeDef NVIC_timer;
	NVIC_timer.NVIC_IRQChannel=TIM5_IRQn;//w razie potrzeby tu zmienic
	NVIC_timer.NVIC_IRQChannelPreemptionPriority=0x00;
	NVIC_timer.NVIC_IRQChannelSubPriority=0x00;
	NVIC_timer.NVIC_IRQChannelCmd=ENABLE;
	NVIC_Init(&NVIC_timer);
	TIM_ITConfig(TIM5, TIM_IT_Update, ENABLE);//i tu tez
}

void TIM5_IRQHandler(void)
{
	if (TIM_GetITStatus(TIM5, TIM_IT_Update) != RESET )
	{
		if(czy_wysylac==-1)//tryb ciagly
		{
			ADC_SoftwareStartConv(ADC1);
			while(ADC_GetFlagStatus(ADC1, ADC_FLAG_EOC)==RESET);
			ADC_Result=ADC_GetConversionValue(ADC1);
			usart_wyslijliczbe(ADC_Result);
		}
		if(czy_wysylac>0)//kalibracja/pomiar
		{
			ADC_SoftwareStartConv(ADC1);
			while(ADC_GetFlagStatus(ADC1, ADC_FLAG_EOC)==RESET);
			ADC_Result=ADC_GetConversionValue(ADC1);
			usart_wyslijliczbe(ADC_Result);
			czy_wysylac--;
		}

		GPIO_ResetBits(GPIOD, GPIO_Pin_12 | GPIO_Pin_13| GPIO_Pin_14| GPIO_Pin_15);
		if(ADC_Result>150)
		{
			GPIO_SetBits(GPIOD, GPIO_Pin_12);
		}
		if(ADC_Result>1000)
		{
			GPIO_SetBits(GPIOD,  GPIO_Pin_12 | GPIO_Pin_13);
		}
		if(ADC_Result>2000)
		{
			GPIO_SetBits(GPIOD, GPIO_Pin_12 | GPIO_Pin_13| GPIO_Pin_14);
		}
		if(ADC_Result>3000)
		{
			GPIO_SetBits(GPIOD, GPIO_Pin_12 | GPIO_Pin_13| GPIO_Pin_14| GPIO_Pin_15);
		}

		// wyzerowanie flagi wyzwolonego przerwania
		TIM_ClearITPendingBit(TIM5, TIM_IT_Update);
	}
}

void przerwanie_przycisk()
{
	NVIC_InitTypeDef NVIC_przycisk;
	NVIC_przycisk.NVIC_IRQChannel=EXTI0_IRQn;
	NVIC_przycisk.NVIC_IRQChannelPreemptionPriority=0x00;
	NVIC_przycisk.NVIC_IRQChannelSubPriority=0x00;
	NVIC_przycisk.NVIC_IRQChannelCmd=ENABLE;
	NVIC_Init(&NVIC_przycisk);

	EXTI_InitTypeDef przerwanie_przycisku;
	przerwanie_przycisku.EXTI_Line=EXTI_Line0;
	przerwanie_przycisku.EXTI_Mode=EXTI_Mode_Interrupt;
	przerwanie_przycisku.EXTI_Trigger=EXTI_Trigger_Rising;
	przerwanie_przycisku.EXTI_LineCmd=ENABLE;
	EXTI_Init(&przerwanie_przycisku);

	SYSCFG_EXTILineConfig(GPIOA, EXTI_PinSource0);
}

void EXTI0_IRQHandler (void)
{
	if(EXTI_GetITStatus(EXTI_Line0)!=RESET)
	{
		EXTI_ClearITPendingBit(EXTI_Line0);
	}
}

void adc()
{
	//zegar dla portu GPIO z ktorego wykorzystany zostanie pin jako wejscie ADC (PA3)
	RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOA , ENABLE );

	//zegar dla modulu ADC1
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_ADC1, ENABLE );

	//inicjalizacja wejscia ADC
	GPIO_InitTypeDef wejscie_adc;
	wejscie_adc. GPIO_Pin = GPIO_Pin_3;//pin 4 - pa4 wspolpracuje z kanalem 4
	wejscie_adc. GPIO_Mode = GPIO_Mode_AN;
	wejscie_adc. GPIO_PuPd = GPIO_PuPd_NOPULL;
	GPIO_Init(GPIOA, &wejscie_adc);

	//wspolna konfiguracja dla wszystkich adc
	ADC_CommonInitTypeDef ADC_wpolna_konfiguracja;
	ADC_wpolna_konfiguracja. ADC_Mode = ADC_Mode_Independent;
	ADC_wpolna_konfiguracja. ADC_Prescaler = ADC_Prescaler_Div2;
	ADC_wpolna_konfiguracja. ADC_DMAAccessMode = ADC_DMAAccessMode_Disabled;
	ADC_wpolna_konfiguracja. ADC_TwoSamplingDelay = ADC_TwoSamplingDelay_5Cycles; //czas przerwy pomiedzy kolejnymi konwersjami
	ADC_CommonInit(&ADC_wpolna_konfiguracja);

	ADC_InitTypeDef ADC_konkretna_konfiguracja;
	ADC_konkretna_konfiguracja. ADC_Resolution = ADC_Resolution_12b;//ustawienie rozdzielczosci przetwornika na maksymalna (12 bitow)
	ADC_konkretna_konfiguracja. ADC_ScanConvMode = DISABLE;
	ADC_konkretna_konfiguracja. ADC_ContinuousConvMode = ENABLE;
	ADC_konkretna_konfiguracja. ADC_ExternalTrigConv = ADC_ExternalTrigConv_T1_CC1;
	ADC_konkretna_konfiguracja. ADC_ExternalTrigConvEdge = ADC_ExternalTrigConvEdge_None;
	ADC_konkretna_konfiguracja. ADC_DataAlign = ADC_DataAlign_Right;//wartosc binarna z wyrownaniem do prawej
	//odczyt stanu przetwornika ADC zwraca wartosc 16bitowa np, wartosc 0xFF wyrownana w prawo to 0x00FF, w lewo 0x0FF0
	ADC_konkretna_konfiguracja. ADC_NbrOfConversion = 1;

	// zapisz wypelniona struktura do rejestrow przetwornika numer 1
	ADC_Init(ADC1, &ADC_konkretna_konfiguracja);
	ADC_RegularChannelConfig(ADC1, ADC_Channel_3, 1, ADC_SampleTime_84Cycles);//UWAGA kanal ADC_Channel_4 i pin musza byc dobrze ustawione
	ADC_Cmd(ADC1, ENABLE );

	/* odczyt wartosci
	ADC_SoftwareStartConv(ADC1);
	while (ADC_GetFlagStatus(ADC1, ADC_FLAG_EOC) == RESET );
	ADC_Result = ADC_GetConversionValue(ADC1);
	*/
}

void usart()
{
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART3, ENABLE);
	RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOC,ENABLE);
	//konfiguracja linii Tx
	GPIO_PinAFConfig(GPIOC, GPIO_PinSource10, GPIO_AF_USART3);
	GPIO_InitTypeDef linia_tx;
	linia_tx.GPIO_OType=GPIO_OType_PP;
	linia_tx.GPIO_PuPd=GPIO_PuPd_UP;
	linia_tx.GPIO_Mode=GPIO_Mode_AF;
	linia_tx.GPIO_Pin=GPIO_Pin_10;
	linia_tx.GPIO_Speed=GPIO_Speed_50MHz;
	GPIO_Init(GPIOC, &linia_tx);

	//konfiguracja linii Rx korzysta z Tx
	GPIO_PinAFConfig(GPIOC,GPIO_PinSource11,GPIO_AF_USART3);
	linia_tx.GPIO_Mode=GPIO_Mode_AF;
	linia_tx.GPIO_Pin=GPIO_Pin_11;
	GPIO_Init(GPIOC, &linia_tx);

	USART_InitTypeDef interfejs_usart;
	interfejs_usart.USART_BaudRate=9600;
	interfejs_usart.USART_WordLength=USART_WordLength_8b;
	interfejs_usart.USART_StopBits=USART_StopBits_1;
	interfejs_usart.USART_Parity=USART_Parity_No;
	interfejs_usart.USART_HardwareFlowControl=USART_HardwareFlowControl_None;
	interfejs_usart.USART_Mode=USART_Mode_Rx|USART_Mode_Tx;
	USART_Init(USART3, &interfejs_usart);

	//wlaczenie usart
	USART_Cmd(USART3, ENABLE);

	//przerwanie usart
	NVIC_InitTypeDef NVIC_USART;
	USART_ITConfig(USART3, USART_IT_RXNE, ENABLE);
	NVIC_USART.NVIC_IRQChannel=USART3_IRQn;
	NVIC_USART.NVIC_IRQChannelPreemptionPriority=0;
	NVIC_USART.NVIC_IRQChannelSubPriority=0;
	NVIC_USART.NVIC_IRQChannelCmd=ENABLE;

	NVIC_Init(&NVIC_USART);
	NVIC_EnableIRQ(USART3_IRQn);
}

int main(void)
{
	SystemInit();

	// ustawienie trybu pracy priorytetow przerwan
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_1);
	usart();
	adc();
	diody();
	timer5();
	przerwanie_timer5();
	for(;;)
	{

	}
}
