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
namespace WinAlcoFon
{
    public sealed partial class MainPage : Page
    {
        int temp = 0;//tylko do rand przydatny
        bool uruchomione;
        private SemaphoreSlim signal = new SemaphoreSlim(0, 1);
        public class dane_do_wykresu
        {
            public int id { get; set; }
            public int wartosc { get; set; }
        }
        public List<dane_do_wykresu> lista_danych = new List<dane_do_wykresu>();
        public MainPage()
        {
            this.InitializeComponent();
            ((LineSeries)LineChart.Series[0]).DependentRangeAxis = new LinearAxis()
            {
                Maximum = 200,
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
        async void szukaj_bt()
        {
            DeviceInformationCollection urzadzenia_bt;
            urzadzenia_bt = await DeviceInformation.FindAllAsync(BluetoothDevice.GetDeviceSelector());
            if (urzadzenia_bt.Count > 0)
            {
                List<string> items = new List<string>();
                foreach (var chatServiceInfo in urzadzenia_bt)
                {
                    items.Add(chatServiceInfo.Name);
                }
                Do_polaczenia.ItemsSource = items;
            }
            else
            {
                List<string> items = new List<string>();
                items.Add("pusto");
                Do_polaczenia.ItemsSource = items;
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
            for(int i=0;i<15;i++)
            {
                lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            }
            (LineChart.Series[0] as LineSeries).ItemsSource = lista_danych;
        }
        private void odswiez_wykres()
        {
            Random rand = new Random();
            lista_danych.RemoveAt(lista_danych.IndexOf(lista_danych.First()));
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = rand.Next(0, 200) });
            (LineChart.Series[0] as LineSeries).Refresh();
        }
        private async void Start_Click(object sender, RoutedEventArgs e)
        {
            blokuj_przyciski(true);
            Stop.IsEnabled = true;
            while (uruchomione)
            {
                odswiez_wykres();
                await Task.Delay(250);
            }
        }
        private void Stop_Click(object sender, RoutedEventArgs e)
        {
            blokuj_przyciski(false);
        }
        private async void Kalibruj_Click(object sender, RoutedEventArgs e)
        {
            //kilka odczytów na suchym powietrzu i usrednienie wyniku
            //wynik odjac od maksimum jakie moze odczytac wynik i podzielic 
            //pomiary otrzymywane przez BT mnozyc przez ostateczny wynik by otrzymać ‰
            blokuj_przyciski(true);
            Wynik_pomocniczy.Text = "Pozostało";
            pomiar();
            await signal.WaitAsync();
            Wynik_pomocniczy.Text = "Czujnik został";
            Wynik.Text = "skalibrowany";
            blokuj_przyciski(false);
            Kalibruj.Content = "Czujnik skalibrowany";
        }
        private async void Pomiar_Click(object sender, RoutedEventArgs e)
        {
            blokuj_przyciski(true);
            Wynik_pomocniczy.Text = "Weź głęboki wdech";
            for (int i = 3; i>=1; i--)
            {
                Wynik.Text = "pomiar za " +i + " sekund";
                await Task.Delay(1000);
            }
            Progres.Value = 0;
            Wynik_pomocniczy.Text = "Dmuchaj przez";
            pomiar();
            await signal.WaitAsync();
            Wynik_pomocniczy.Text = "Twój wynik to";
            Wynik.Text = "X ‰";
            blokuj_przyciski(false);
        }
        async void pomiar()
        {
            int ilosc_probek=20;
            Progres.Value = 0;
            for (int i=0;i< ilosc_probek; i++)
            {
                Wynik.Text =(ilosc_probek/4-i/4)+" sekund";
                Progres.Value += (float)100/ilosc_probek;
                await Task.Delay(250);
            }
            signal.Release();
        }
        private void Polacz_Click(object sender, RoutedEventArgs e)
        {
            blokuj_przyciski(false);
            Polacz.Content = "Połączono";
            Do_polaczenia.IsEnabled = false;
            Polacz.IsEnabled = false;
            Odswiez.IsEnabled = false;
        }
        private void Odswiez_Click(object sender, RoutedEventArgs e)
        {
            szukaj_bt();
        }
    }
}
