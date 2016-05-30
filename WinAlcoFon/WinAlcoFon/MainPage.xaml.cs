using System;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Generic;
using System.Linq;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Input;
using WinRTXamlToolkit.Controls.DataVisualization.Charting;
using Windows.Devices.Bluetooth.Rfcomm;
using Windows.Devices.Enumeration;
using Windows.Storage.Streams;
using Windows.Networking.Sockets;

namespace WinAlcoFon
{
    public sealed partial class MainPage : Page
    {
        class dane_do_wykresu
        {
            public double id { get; set; }
            public double wartosc { get; set; }
        }
        double temp = 0;//do wartosci wyswietlanych pod osia OX
        bool uruchomione;
        bool polaczono;
        double wynik_w_promilach = 0;
        double usredniony_wynik = 0;
        double prog_0_promili = 150;//zmieniany podczas kalibracji
        double przelicznik = 0.0021287379624937;//zmieniany podczas kalibracji, poczatkowo obliczony dla progu=150, wyliczany z wzoru 8.4/(4096-prog_0_promili)
        //gdzie 8.4 promila to maksymalny odczyt z czujnika MQ-3 według specyfikacji
        SemaphoreSlim signal = new SemaphoreSlim(0, 1);
        List<dane_do_wykresu> lista_danych = new List<dane_do_wykresu>();
        DeviceInformationCollection urzadzenia_bt;
        DeviceInformation urzadzenie_bt_do_polaczenia;
        RfcommDeviceService _service;
        StreamSocket _socket;
        DataWriter WriterData;
        DataReader ReaderData;
        public MainPage()
        {
            this.InitializeComponent();
            ((LineSeries)LineChart.Series[0]).DependentRangeAxis = new LinearAxis()
            {
                Orientation = AxisOrientation.Y,
                ShowGridLines = true,
                UseLayoutRounding=true
            };
            zaladuj_wykres();
            blokuj_przyciski(true);
            image.IsTapEnabled = true;
            szukaj_bt();
        }
        void wypisz(string gora, string dol = "AlcoFon®")
        {
            if (gora != "")
            {
                tekst_pomocniczy.Text = gora;
            }
            if (dol != "")
            {
                Wynik.Text = dol;
            }
        }
        async Task<int> pojedynczy_odczyt()
        {
            try
            {
                byte[] b = new byte[2];
                await ReaderData.LoadAsync(2);
                b[0] = ReaderData.ReadByte();
                b[1] = ReaderData.ReadByte();
                int pomiar = b[0] * 256 + b[1];
                return pomiar;
            }
            catch
            {
                wypisz("Wystąpił błąd w transmisji danych, wykonaj pomiar jeszce raz", "!BŁĄD!");
                return 0;
            }
        }
        async void szukaj_bt()
        {
            urzadzenia_bt = await DeviceInformation.FindAllAsync(RfcommDeviceService.GetDeviceSelector(RfcommServiceId.SerialPort));
            List<string> items = new List<string>();
            if (urzadzenia_bt.Count > 0)
            {
                Polacz.IsEnabled = true;
                foreach (var chatServiceInfo in urzadzenia_bt)
                {
                    items.Add(chatServiceInfo.Name);
                    wypisz("Aby rozpocząć pomiary połącz się z urządzeniem zgodnym z");
                }
            }
            else
            {
                Polacz.IsEnabled = false;
                items.Add("Nie znaleziono zadnego urzadzenia");
                wypisz("Pamiętaj by wcześniej sparować urządzenie i włączyć Bluetooth");
            }
            Do_polaczenia.ItemsSource = items;
            Do_polaczenia.SelectedIndex = 0;
        }
        void wyslij_przez_bt(string msg)
        {
            try
            {
                WriterData.WriteString(msg);
                WriterData.StoreAsync();
            }
            catch
            {
                wypisz("Wystąpił błąd podczas wysyłania danych", "!BŁĄD!");
            }
        }
        void blokuj_przyciski(bool zablokowane)
        {
            if (zablokowane)
            {
                Start.IsEnabled = false;
                Stop.IsEnabled = false;
                Pomiar.IsEnabled = false;
                Kalibruj.IsEnabled = false;
                uruchomione = true;
                image.IsTapEnabled = false;
            }
            else
            {
                Start.IsEnabled = true;
                Stop.IsEnabled = false;//domyslnie wylaczony
                Pomiar.IsEnabled = true;
                Kalibruj.IsEnabled = true;
                uruchomione = false;
                image.IsTapEnabled = true;
            }
        }
        void zaladuj_wykres()
        {
            temp = 0;
            lista_danych.Clear();
            Random rand = new Random();
            for (double i = -4; i <= 0; i += 0.25)
            {
                lista_danych.Add(new dane_do_wykresu() { id = i, wartosc = 0 });
            }
            (LineChart.Series[0] as LineSeries).ItemsSource = lista_danych;
        }
        async void odswiez_wykres()
        {
            lista_danych.RemoveAt(lista_danych.IndexOf(lista_danych.First()));
            temp += 0.25;
            double wart = Math.Round((await pojedynczy_odczyt() - prog_0_promili) * przelicznik, 2);
            lista_danych.Add(new dane_do_wykresu() { id = temp, wartosc = wart });
            (LineChart.Series[0] as LineSeries).Refresh();
        }
        async void pomiar()
        {
            int suma = 0;
            int ilosc_probek = 20;
            Progres.Value = 0;
            wyslij_przez_bt("p");
            for (int i = 0; i < ilosc_probek; i++)
            {
                wypisz("", (ilosc_probek / 4 - i / 4) + " sekund");
                Progres.Value += (float)100 / ilosc_probek;
                suma += await pojedynczy_odczyt();
                await Task.Delay(250);
            }
            usredniony_wynik = (double)suma / ilosc_probek;
            wynik_w_promilach = Math.Round((usredniony_wynik - prog_0_promili) * przelicznik, 3);
            signal.Release();
        }
        async void Start_Click(object sender, RoutedEventArgs e)
        {
            zaladuj_wykres();
            blokuj_przyciski(true);
            wypisz("\nTryb ciągłego odczytu rozpoczęty");
            Stop.IsEnabled = true;
            while(uruchomione)
            {
                wyslij_przez_bt("t");
                await Task.Delay(100);
                odswiez_wykres();
                await Task.Delay(100);
            }
        }
        void Stop_Click(object sender, RoutedEventArgs e)
        {
            wyslij_przez_bt("s");
            wypisz("\nTryb ciągłego odczytu zakończony");
            blokuj_przyciski(false);
        }
        async void Kalibruj_Click(object sender, RoutedEventArgs e)
        {
            blokuj_przyciski(true);
            wypisz("\nPozostało", "");
            pomiar();
            await signal.WaitAsync();
            wypisz("\nCzujnik został skalibrowany", "na 0.00‰");
            prog_0_promili = usredniony_wynik;
            przelicznik = 8.4 / (4096 - prog_0_promili);
            blokuj_przyciski(false);
            Kalibruj.Content = "Ponowna kalibracja";
        }
        async void Pomiar_Click(object sender, RoutedEventArgs e)
        {
            blokuj_przyciski(true);
            for (int i = 3; i >= 1; i--)
            {
                wypisz("Weź głęboki wdech,\npomiar za ", i + " sekund");
                await Task.Delay(1000);
            }
            wypisz("\nDmuchaj przez");
            pomiar();
            await signal.WaitAsync();
            if (wynik_w_promilach >= 0)
            {
                wypisz("\nWynik pomiaru to:", wynik_w_promilach + " ‰");
            }
            else if (wynik_w_promilach < -0.01)
            {
                wypisz("Czujnik był niegotowy lub był źle skalibrowany, stąd wynik to:", wynik_w_promilach + " ‰");
            }
            else
            {
                wypisz("\nWynik pomiaru to:", "0.00 ‰");
            }
            blokuj_przyciski(false);
        }
        async void Polacz_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                urzadzenie_bt_do_polaczenia = urzadzenia_bt.Single(x => x.Name == Do_polaczenia.SelectedValue.ToString());
                _service = await RfcommDeviceService.FromIdAsync(urzadzenie_bt_do_polaczenia.Id);
                _socket = new StreamSocket();
                Polacz.IsEnabled = false;
                Odswiez.IsEnabled = false;
                Do_polaczenia.IsEnabled = false;
                Polacz.Content = "Łączenie";
                await _socket.ConnectAsync(_service.ConnectionHostName, _service.ConnectionServiceName, SocketProtectionLevel.BluetoothEncryptionAllowNullAuthentication);
                blokuj_przyciski(false);
                Polacz.Content = "Połączono";
                Do_polaczenia.IsEnabled = false;
                Polacz.IsEnabled = false;
                Odswiez.IsEnabled = false;
                wypisz("Połączenie zostało nawiązane,\nteraz można rozpocząć pomiary z");
                WriterData = new DataWriter(_socket.OutputStream);
                ReaderData = new DataReader(_socket.InputStream);
                polaczono = true;
            }
            catch (Exception)
            {
                Odswiez.IsEnabled = true;
                Do_polaczenia.IsEnabled = true;
                Polacz.Content = "Połącz";
                Polacz.IsEnabled = true;
                wypisz("Sprawdź wybrane urządzenie i czy jest ono dostępne");
            }
        }
        void Odswiez_Click(object sender, RoutedEventArgs e)
        {
            szukaj_bt();
        }
        async void image_Tapped(object sender, TappedRoutedEventArgs e)
        {
            try
            {
                String wiadomosc_popup = "";
                if (polaczono)
                {
                    wyslij_przez_bt("t");
                    await Task.Delay(100);
                    int aktualny_odczyt = await pojedynczy_odczyt();
                    wiadomosc_popup = "\nAktualny odczyt to: " + aktualny_odczyt + "\n";
                    if (aktualny_odczyt <= 150)
                    {
                        wiadomosc_popup += "Wszystko jest OK, pomiary będą całkiem dokładne.";
                    }
                    else if (aktualny_odczyt > 150 && aktualny_odczyt < 225)
                    {
                        wiadomosc_popup += "Nie jest tak źle, ale lepiej zaczekać jeszcze chwilę by uzyskać dokładniejsze pomiary.";
                    }
                    else
                    {
                        wiadomosc_popup += "Pomiary będą bardzo nie dokładne, stanowczo trzeba zaczekać.";
                    }
                }
                var dialog = new Windows.UI.Popups.MessageDialog(
                    "Między pomiarami powinna nastąpić przerwa by czujnik mógł się znów \"przyzwyczaić\" do świeżego powietrza. Należy pamiętać, że jest to prosty czujnik i dokładność pomiarów może nie odzwierciedlać rzeczywistości. Wynik zależy nawet od wilgotności i temperatury w jakiej sprzęt jest używany. NIGDY NIE PROWADŹ SAMOCHODU PO SPOŻYCIU ALKOHOLU\n" +
                    "Dane do debugowania:" + "\n" +
                    "próg: " + prog_0_promili + '\n' +
                    "przelicznik: " + przelicznik + '\n' +
                    "nieprzeliczony ostatni pomiar: " + usredniony_wynik +
                    wiadomosc_popup,
                    "Informacje ogólne i uwagi");
                dialog.Commands.Add(new Windows.UI.Popups.UICommand("OK") { Id = 0 });
                var result = await dialog.ShowAsync();
            }
            catch (Exception) { }
        }
    }
}
