#pragma once

#define APP_WIDTH 500
#define APP_HEIGHT 450

#define APP_MARGIN 10

#define TIMER_DEVICE_CHECK 2001

#include <wx/wx.h>
#include <wx/notebook.h>
#include <wx/msgdlg.h>
#include <wx/hyperlink.h>

#include "gui/wxdevicelist.h"
#include "settingsmanager.h"
#include "logger.h"
#include "gui/taskbar.h"

#define CHK_STARTUP 1001
#define CHK_MINTASK 1002
#define SL_MOVE 1003
#define SL_SCROLL 1004

#define HOST_NAME_TXT 1010
#define HOST_IP_TXT 1011

class Server; // Forward declaration

class wxMain: public wxFrame
{
    public:
        wxDeviceList *wxdevlist = nullptr;
        wxPanel *tab_log = nullptr;

        SettingsManager &settings;
        Server *server = nullptr; // Set by wxApplication after construction

        wxMain(SettingsManager &_settings);
        ~wxMain();
    
        void SetHostInfo(std::string _Hostname, std::string _IpAddress);

        void UpdateUI();
        
    private:
        wxPanel *tab_status = nullptr;
        wxPanel *tab_settings = nullptr;
        wxNotebook *wxnotebook = nullptr;
        wxMTaskbar *tb = nullptr;
        
        wxStaticText *deviceStatusText = nullptr;
        wxStaticText *connectionStatusText = nullptr;
        wxTimer deviceCheckTimer;
        
        wxDECLARE_EVENT_TABLE();    

        void InitStatusTab();   
        void InitLogTab();
        void InitSettingsTab();

        void CheckboxCmdHandler(wxCommandEvent &evt);
        void SliderCmdHandler(wxScrollEvent &evt);
        void OnDeviceCheckTimer(wxTimerEvent &evt);

        void HideWindow(wxIconizeEvent &evt);
        void CloseWindow(wxCloseEvent &evt);
};
