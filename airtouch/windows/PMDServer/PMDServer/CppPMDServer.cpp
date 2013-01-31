#include "simplewinsock.h"

#include <time.h>

#include "PMDCamera.h"
#include "PMDUtils.h"
#define NETWORK_DEBUG

/* Globar vars */
// Sent over network
PMDCamera _pmdCamera;
PMDData _pmdData;
HANDLE _pmdDataMutex;
PMDRequest _fromClient;

// UI
IplImage* _distancesAndFingerLocation;

/* function declarations */

// UI
void welcomeMessage();


bool _stayAlive = true;

// FPS
time_t _fpsStart, _fpsEnd;
double _fps = 0;
int _fpsCounter = 0;

// Random
void error(string msg);

// Networking
bool communicateWithClient(SOCKET* hClient);

void error(string msg)
{
	fprintf(stderr, "%s\n", msg);
	exit(EXIT_FAILURE);
}

//
// UI
//
void welcomeMessage()
{
	// add welcome here
	cout << "PMDServer sends data from pmd camera or a file" << endl;
	cout << "Usage: PMDServer.exe to read from camera" << endl;
	cout << "Usage: PMDServer.exe [--use-ir-tracker] [filename.pmd] to read from .pmd file" << endl;
}

bool fingerCompare(Finger a, Finger b){ return a.id < b.id;}

void updateUI()
{
	// calculate current fps
	time(&_fpsEnd);
	
	double sec = difftime(_fpsEnd, _fpsStart);
	if(sec > 1)
	{
		_fps = _fpsCounter;
		time(&_fpsStart);
		_fpsCounter = 0;
	} else
	{
		++_fpsCounter;
	}

	PMDUtils::DistancesToImage(_pmdCamera.GetDistancesProcessed(), _distancesAndFingerLocation);

	vector<Finger> fingers = _pmdCamera.GetFingers();
	std::sort(fingers.begin(), fingers.end(), fingerCompare);

	CvScalar fingerColors[2] = {CV_RGB(255,0,0), CV_RGB(0,0,255)};
	for(vector<Finger>::iterator i = fingers.begin(); i !=fingers.end(); i++)
	{
		cvCircle(_distancesAndFingerLocation, i->screenCoords, 10, fingerColors[i - fingers.begin()]);
	}

    cvFlip (_distancesAndFingerLocation, _distancesAndFingerLocation, -1);

	ostringstream str;
	str.precision(2);
	str << "fps: " << _fps;
    // Display the image
	// to do: use consistent method calls (all 2.1 or all 1.*)
	Mat img = _distancesAndFingerLocation;
	putText(img, str.str(), cvPoint(10,20), FONT_HERSHEY_COMPLEX_SMALL, 1.0f,CV_RGB(255,0,0)) ;
	cvShowImage ("Server", _distancesAndFingerLocation);
	
	cvWaitKey (1);
}


void showBackgroundImage()
{
	// todo: convert to a method (repeated once already)
	BackgroundSubtractionData const* const backgroundData = _pmdCamera.GetBackgroundSubtractionData();
	IplImage* toShow = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), 8, 3);
	PMDUtils::DistancesToImage(backgroundData->means, toShow);
	
	cvFlip (toShow, toShow, 0);

    // Display the image
    cvShowImage ("Background Image", toShow);
	cvWaitKey(1);

	cvReleaseImage(&toShow);

	//// show standard deviation
	toShow = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), 8, 3);
	PMDUtils::DistancesToImage(backgroundData->stdevs, toShow);

	cvFlip (toShow, toShow, 0);
	cvShowImage ("Stdev Image", toShow);
	cvWaitKey(1);
	cvReleaseImage(&toShow);
}

// Initializes connection to client and sends data.
// Returns when client disconnects
// Returns true if client sends a shutdown message.
bool communicateWithClient(SOCKET* hClient)
{
	bool result = true;
	// receive params about client 
	memset(_pmdData.buffer, 0, BUFSIZE);
	memset(_fromClient.buffer, 0, BUFSIZE);

	cout << "receiving initial handshake..." << endl;

	int bytesReceived = receiveData(*hClient, _fromClient.buffer, 512, false);

	// check input data
	if(!SUCCEEDED(result))
	{
		cout << "failed to receive data from client, disconnecting..." << endl;
		return true;
	}
	
	cout << "client first message: " << _fromClient.buffer << endl;

	// send a reply, simply echo for now
	HRESULT hr = sendData(*hClient, _fromClient.buffer, BUFSIZE, 0);
	if(!SUCCEEDED(hr)) return true;



	while(true)
	{
		// receive data from client
		// check first int, if 'd' then disconnect
		// if 'q' then shutdown
		bytesReceived = receiveData(*hClient, _fromClient.buffer, 512, false);
		if(bytesReceived == 0) return true;
		// write the rest of the data send to the screen

#ifdef NETWORK_DEBUG
		cout << "received: " << (_fromClient.buffer) << endl;
#endif

		char command = _fromClient.buffer[0];

		if(command == 'q') return false;
		if(command == 'd') return true;
		
		// send the data
		// make sure to lock it so it doesn't get overridden in the middle
		if(command == 'f')
		{
			// only send the finger data
			WaitForSingleObject(_pmdDataMutex, INFINITE);
			hr = sendData(*hClient, (char*)&_pmdData, sizeof(PMDFingerData), 0);	
			ReleaseMutex(_pmdDataMutex);
			
#ifdef NETWORK_DEBUG
		cout << "sent finger data "<< endl;
#endif

		} else
		{
			WaitForSingleObject(_pmdDataMutex, INFINITE);
			hr = sendData(*hClient, (char*)&_pmdData, sizeof(PMDData), 0);	
			ReleaseMutex(_pmdDataMutex);
		}
		

		if(!SUCCEEDED(hr)) return true;

	}

	// close client connection 

	return result;
}

DWORD WINAPI doNetworkCommunication(LPVOID lpParam)
{
	WSADATA wsaData = {0};
	WORD wVer = MAKEWORD(2,2);

	// Step 1: initialize the socket
	HRESULT hr = initWinsock(&wsaData, &wVer);
	if(!SUCCEEDED(hr)) return -1;

	cout << "Starting server on socket 10000..." << endl;

	SOCKET hSock  = {0};
	hSock = socket( AF_INET, SOCK_STREAM, IPPROTO_IP );
	if( hSock == INVALID_SOCKET ) { 
		error("Invalid socket, failed to create socket");
	}

	// step 3: bind socket
	sockaddr_in saListen = {0};
	saListen.sin_family      = PF_INET;    
	saListen.sin_port        = htons( 10000 );     
	saListen.sin_addr.s_addr = htonl( INADDR_ANY );  
	// bind socket's name 

	int flag = 1;
	hr = setsockopt(hSock, IPPROTO_TCP, TCP_NODELAY, (char*)&flag, sizeof(int));
	if(!SUCCEEDED(hr)) return -1;

	hr = bindSocket(&hSock, &saListen);
	if(!SUCCEEDED(hr)) return -1;

	// listen for incoming connections until force kill
	while( _stayAlive )
	{
		cout << "Listening for connections on port 10000...." << endl;

		// step 4: listen for a client 
		SOCKET hClient;
		hr = getClientConnection(&hSock, &hClient);
		if(!SUCCEEDED(hr)) error("Error: Failed to get client connection");

		_stayAlive = communicateWithClient(&hClient);
		closesocket( hClient );
		hClient = 0;
	}
	cout << "Shutting down the server" << endl;

	// close server socket 
	hr = closesocket( hSock );
	hSock = 0;
	if( hr == SOCKET_ERROR )  cout << "Error: failed to close socket" << endl;


	// Release WinSock DLL 
	hr = WSACleanup();
	if( hr == SOCKET_ERROR ) cout << "Error: cleaning up Winsock Library" << endl;

}

int main(int argc, char* argv[])
{
	// welcome message
	welcomeMessage();
	
	// initialize opencv frame
	_distancesAndFingerLocation = cvCreateImage(cvSize(PMDNUMCOLS,PMDNUMROWS), 8, 3);

	HRESULT hr = 0;

	// Initialize the PMD camera
	// check if we have parameters, if so first param is filename
	int argi = 1;
	bool fromCamera = true;
	while(argc > argi)
	{
		if(strcmp(argv[argi], "--use-ir-tracker") == 0)
		{
			_pmdCamera.m_useIrTracker = true;
			cout << "using ir tracker" << endl;
		} else  if(argi == argc - 1)
		{
			char* filename = argv[argi];

			cout << "Reading data from file " << filename << endl;
			hr = _pmdCamera.InitializeCameraFromFile(filename);
			if(!SUCCEEDED(hr)) error("Error: failed to initialize from file");
			fromCamera = false;
		} else 
		{
			cout << "Unknown argument: " << argv[argi] << endl;
		}
		argi++;

	}

	if(fromCamera)
	{
		hr = _pmdCamera.InitializeCamera();
		if(!SUCCEEDED(hr)) error("Error: failed to initialize PMD camera");
	}

	hr = _pmdCamera.InitializeBackgroundSubtraction();
	if(!SUCCEEDED(hr)) error("Error: Background subtraction failed");
	
	// initialize the pmd data mutex
	_pmdDataMutex = CreateMutex(NULL, FALSE, NULL);


	// start the network thread
	cout << "Starting network thread..." << endl;
	HANDLE networkThread = CreateThread(NULL, 0, doNetworkCommunication, NULL, 0, 0);

	while(_stayAlive)
	{
		// update data
		hr = _pmdCamera.UpdateCameraData();

		_pmdCamera.UpdateFingers();
		
		// lock the pmd data
		WaitForSingleObject(_pmdDataMutex, INFINITE);
		// update the pmddata to send
		memset(&_pmdData, 0, sizeof(PMDData));
		memcpy(&_pmdData, &_pmdCamera.GetFingerData(), sizeof(float) * 6);
		memcpy_s(_pmdData.buffer,_countof(_pmdData.buffer) * sizeof(float), _pmdCamera.GetDistanceBuffer(), _countof(_pmdData.buffer) * sizeof(float));
		// unlock pmd data
		ReleaseMutex(_pmdDataMutex);

		updateUI();

		// draw ui
	}

	cvReleaseImage (&_distancesAndFingerLocation);
	
	cout << "Waiting for network thread to die..." << endl;
	WaitForSingleObject(networkThread, INFINITE);
	
	CloseHandle(_pmdDataMutex);

}