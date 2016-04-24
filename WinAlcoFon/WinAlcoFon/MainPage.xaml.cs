using System;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;
using WinRTXamlToolkit.Controls.DataVisualization.Charting;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.Rfcomm;
using Windows.Devices.Enumeration;
using Windows.Devices.Enumeration.Pnp;
using Windows.Storage.Streams;
using Windows.Networking.Sockets;

namespace WinAlcoFon
{
    public sealed partial class MainPage : Page
    {
        private class dane_do_wykresu
        {
            public double id { get; set; }
            public int wartosc { get; set; }
        }
        double temp = 0;//tylko do rand przydatny
        bool uruchomione;
        double wynik_w_promilach;
        double sredni_wynik;
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
                //Maximum = 200,
                Minimum = 0,
                Orientation = AxisOrientation.Y,
                ShowGridLines = true
            };
            this.zaladuj_wykres();
            uruchomione = false;
            blokuj_przyciski(true);
            Stop.IsEnabled = false;
            szukaj_bt();
        }
        void podglad()
        {
            //odczyty.Text = "ostatni pomiar: " + pomiar.ToString() + '\n' + "prog 0: " + prog_0_promili + '\n' + "przelicznik: " + przelicznik;
        }
        private async Task<int> pojedynczy_odczyt()
        {
            byte[] b = new byte[4];
            await ReaderData.LoadAsync(4);
            b[0] = ReaderData.ReadByte();
            b[1] = ReaderData.ReadByte();
            b[2] = ReaderData.ReadByte();
            b[3] = ReaderData.ReadByte();
            int pomiar = Int32.Parse("" + (char)b[0] + (char)b[1] + (char)b[2] + (char)b[3]);
            return pomiar;
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
                    Wynik_pomocniczy.Text = "Aby rozpocząć pomiary połącz się z urządzeniem zgodnym z";
                }
            }
            else
            {
                Polacz.IsEnabled = false;
                items.Add("Nie znaleziono zadnego urzadzenia");
                Wynik_pomocniczy.Text = "Pamiętaj by wcześniej sparować urządzenie i włączyć Bluetooth";
            }
            Do_polaczenia.ItemsSource = items;
            Do_polaczenia.SelectedIndex = 0;
        }
        void wyslij_przez_bt(string msg)
        {
            try
            {
                WriterData.WriteUInt32((uint)1);
                WriterData.WriteString(msg);
                WriterData.StoreAsync();
            }
            catch (Exception)
            {
                Wynik_pomocniczy.Text = "Wystąpił błąd podczas wysyłania danych";
            }
        }
        void blokuj_przyciski(bool zablokowane)
        {
            if(zablokowane)
            {
                Start.IsEnabled = false;
                Stop.IsEnabled = false;
                Pomiar.IsEnabled = false;
                Kalibruj.IsEnabled = false;
                uruchomione = true;
            }
            else
            {
                Start.IsEnabled = true;
                Stop.IsEnabled = false;//domyslnie wylaczony
                Pomiar.IsEnabled = true;
                Kalibruj.IsEnabled = true;
                uruchomione = false;
            }
        }
        void zaladuj_wykres()
        {
            Random rand = new Random();
            for(double i=-3.75;i<=0;i+=0.25)
            {
                lista_danych.Add(new dane_do_wykresu() { id = i, wartosc = 0 });
            }
            (LineChart.Series[0] as LineSeries).ItemsSource = lista_danych;
        }
        async void odswiez_wykres()
        {
            Random rand = new Random();
            lista_danych.RemoveAt(lista_danych.IndexOf(lista_danych.First()));
            temp += 0.25;
            lista_danych.Add(new dane_do_wykresu() { id = temp, wartosc = await pojedynczy_odczyt() });
            (LineChart.Series[0] as LineSeries).Refresh();
        }
        async void pomiar()
        {
            int suma = 0;
            int ilosc_probek = 20;
            Progres.Value = 0;
            wyslij_przez_bt("P");
            for (int i = 0; i < ilosc_probek; i++)
            {
                Wynik.Text = (ilosc_probek / 4 - i / 4) + " sekund";
                Progres.Value += (float)100 / ilosc_probek;
                suma += await pojedynczy_odczyt();
                await Task.Delay(250);
            }
            sredni_wynik = suma / ilosc_probek;
            wynik_w_promilach = Math.Round((sredni_wynik - prog_0_promili) * przelicznik, 3);
            if (wynik_w_promilach < 0)
            {
                wynik_w_promilach = 0;
            }
            signal.Release();
        }
        private async void Start_Click(object sender, RoutedEventArgs e)
        {
            blokuj_przyciski(true);
            Stop.IsEnabled = true;
            wyslij_przez_bt("C");
            while (uruchomione)
            {
                odswiez_wykres();
                await Task.Delay(250);
            }
        }
        private void Stop_Click(object sender, RoutedEventArgs e)
        {
            blokuj_przyciski(false);
            wyslij_przez_bt("S");
        }
        private async void Kalibruj_Click(object sender, RoutedEventArgs e)
        {
            //kilka odczytów na suchym powietrzu i usrednienie wyniku
            //wynik odjac od maksimum jakie moze odczytac wynik i podzielic 
            //pomiary otrzymywane przez BT mnozyc przez ostateczny wynik by otrzymać ‰
            blokuj_przyciski(true);
            Wynik_pomocniczy.Text = "\nPozostało";
            pomiar();
            await signal.WaitAsync();
            Wynik_pomocniczy.Text = "\nCzujnik został skalibrowany";
            Wynik.Text = "na 0.00‰";

            prog_0_promili = sredni_wynik;
            przelicznik = 8.4 / (4096 - prog_0_promili);
            blokuj_przyciski(false);
            Kalibruj.Content = "Czujnik skalibrowany";
        }
        private async void Pomiar_Click(object sender, RoutedEventArgs e)
        {
            blokuj_przyciski(true);
            Wynik_pomocniczy.Text = "Weź głęboki wdech,\npomiar za ";
            for (int i = 3; i>=1; i--)
            {
                Wynik.Text = i + " sekund";
                await Task.Delay(1000);
            }
            Wynik_pomocniczy.Text = "\nDmuchaj przez";
            pomiar();
            await signal.WaitAsync();
            Wynik_pomocniczy.Text = "\nWynik pomiaru to:";
            Wynik.Text = wynik_w_promilach+" ‰";
            blokuj_przyciski(false);
        }
        private async void Polacz_Click(object sender, RoutedEventArgs e)
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
                await _socket.ConnectAsync(_service.ConnectionHostName,_service.ConnectionServiceName,SocketProtectionLevel.BluetoothEncryptionAllowNullAuthentication);
                blokuj_przyciski(false);
                Polacz.Content = "Połączono";
                Do_polaczenia.IsEnabled = false;
                Polacz.IsEnabled = false;
                Odswiez.IsEnabled = false;
                Wynik_pomocniczy.Text = "Połączenie zostało nawiązane\nTeraz można rozpocząć pomiary z";
                WriterData = new DataWriter(_socket.OutputStream);
                ReaderData = new DataReader(_socket.InputStream);
            }
            catch (Exception)
            {
                Odswiez.IsEnabled = true;
                Do_polaczenia.IsEnabled = true;
                Polacz.Content = "Połącz";
                Polacz.IsEnabled = true;
                Wynik_pomocniczy.Text = "Sprawdź wybrane urządzenie i czy jest ono dostępne";
            }
        }
        private void Odswiez_Click(object sender, RoutedEventArgs e)
        {
            szukaj_bt();
        }
    }
}
