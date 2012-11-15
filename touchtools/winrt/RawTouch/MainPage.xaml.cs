using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using Windows.Devices.Input;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.UI;
using Windows.UI.Input;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;
using Windows.UI.Xaml.Shapes;

// The Blank Page item template is documented at http://go.microsoft.com/fwlink/?LinkId=234238

namespace RawTouch
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        private Dictionary<uint, PointerPoint> _contacts;
        Rect _rect = new Rect();
        private Dictionary<uint, Ellipse> _ellipses; 
        public MainPage()
        {
            this.InitializeComponent();
            _contacts = new Dictionary<uint, PointerPoint>();
            _ellipses = new Dictionary<uint, Ellipse>();
            PointerPressed += OnPointerUpdated;
            PointerMoved += OnPointerUpdated;
            PointerReleased += OnPointerLost;
            PointerExited += OnPointerLost;
            PointerCaptureLost += OnPointerLost;
        }


        private void OnPointerLost(object sender, PointerRoutedEventArgs args)
        {
            if (_contacts.ContainsKey(args.Pointer.PointerId))
            {
                _contacts.Remove(args.Pointer.PointerId);

                LayoutRoot.Children.Remove(_ellipses[args.Pointer.PointerId]);
                _ellipses.Remove(args.Pointer.PointerId);
                
            }
            UpdateUI();
        }

        private void OnPointerUpdated(object sender, PointerRoutedEventArgs args)
        {
            // add to the list
            uint id = args.Pointer.PointerId;
            if(!_contacts.ContainsKey(id))
            {
                _ellipses[id] = new Ellipse();    
                LayoutRoot.Children.Add(_ellipses[id]);
            }
            _contacts[id] = args.GetCurrentPoint(this);
            UpdateUI();
            // redraw
        }

        
        private void UpdateUI()
        {
            foreach (var kv in _contacts)
            {
                uint id = kv.Key;
                PointerPoint p = kv.Value;

                Ellipse el = _ellipses[id];

                if(p.PointerDevice.PointerDeviceType == PointerDeviceType.Touch )
                {
                    el.Fill = new SolidColorBrush(Colors.Pink);
                    // unfortunately contact area and tilt are not at all informative on surface
                    // also max # touch points is 5
                    //Debug.WriteLine("contactrect {0}", p.Properties.ContactRect);
                    //Debug.WriteLine("rawrect {0}", p.Properties.ContactRectRaw);
                    //Debug.WriteLine("orientation {0}", p.Properties.Orientation);
                    //Debug.WriteLine("xTilt {0} yTilt {1}", p.Properties.XTilt, p.Properties.YTilt);
                    _rect.Width = p.Properties.ContactRect.Width * 50;
                    _rect.Height = p.Properties.ContactRect.Height * 50;
                    
                    
                } else
                {
                    _rect.Width = 30;
                    _rect.Height = 30;
                    el.Fill = new SolidColorBrush(Colors.Gray);
                }
                el.Width = _rect.Width;
                el.Height = _rect.Height;
                Canvas.SetLeft(el, p.Position.X - _rect.Width / 2 - 100);
                Canvas.SetTop(el, p.Position.Y - _rect.Height / 2 - 100);
            }
        }

        /// <summary>
        /// Invoked when this page is about to be displayed in a Frame.
        /// </summary>
        /// <param name="e">Event data that describes how this page was reached.  The Parameter
        /// property is typically used to configure the page.</param>
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
        }
    }
}
