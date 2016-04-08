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

// The Blank Page item template is documented at http://go.microsoft.com/fwlink/?LinkId=402352&clcid=0x409

namespace WinAlcoFon
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        int temp = 0;
        bool dziala;
        public class dane_do_wykresu
        {
            public int id { get; set; }
            public int wartosc { get; set; }
        }
        public List<dane_do_wykresu> lista_danych = new List<dane_do_wykresu>();
        public MainPage()
        {
            this.InitializeComponent();
            this.zaladuj_wykres();
            dziala = false;
            Zatrzymaj.IsEnabled = false;
        }
        void zaladuj_wykres()
        {
            Random rand = new Random();
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = 0 });
            (LineChart.Series[0] as LineSeries).ItemsSource = lista_danych;
        }
        private void odswiez_wykres()
        {
            Random rand = new Random();
            lista_danych.RemoveAt(lista_danych.IndexOf(lista_danych.First()));
            lista_danych.Add(new dane_do_wykresu() { id = temp++, wartosc = rand.Next(0, 200) });
            (LineChart.Series[0] as LineSeries).Refresh();
        }
        private async void Odswiez_Click(object sender, RoutedEventArgs e)
        {
            Odswiez.IsEnabled = false;
            Zatrzymaj.IsEnabled = true;
            dziala = true;
            while (dziala)
            {
                odswiez_wykres();
                await Task.Delay(250);
            }
        }

        private void Zatrzymaj_Click(object sender, RoutedEventArgs e)
        {
            Odswiez.IsEnabled = true;
            Zatrzymaj.IsEnabled = false;
            dziala = false;
        }
    }
}
