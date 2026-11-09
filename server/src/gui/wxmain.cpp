#include "wxmain.h"
#include "net/server.h"

#ifdef _WIN32
#include "adb.hpp"
#endif

#include "../res/icon.xpm"

wxBEGIN_EVENT_TABLE(wxMain, wxFrame)
    EVT_ICONIZE(wxMain::HideWindow)
    EVT_CHECKBOX(CHK_STARTUP, wxMain::CheckboxCmdHandler)
    EVT_CHECKBOX(CHK_MINTASK, wxMain::CheckboxCmdHandler)
    EVT_COMMAND_SCROLL(SL_MOVE, wxMain::SliderCmdHandler)
    EVT_COMMAND_SCROLL(SL_SCROLL, wxMain::SliderCmdHandler)
    EVT_TIMER(TIMER_DEVICE_CHECK, wxMain::OnDeviceCheckTimer)
wxEND_EVENT_TABLE()

wxMain::wxMain(SettingsManager &_settings) : 
    wxFrame(NULL, wxID_ANY, "Pocket Pad", wxDefaultPosition, wxSize(APP_WIDTH, APP_HEIGHT), wxDEFAULT_FRAME_STYLE & ~wxRESIZE_BORDER & ~wxMAXIMIZE_BOX),
    settings(_settings),
    deviceCheckTimer(this, TIMER_DEVICE_CHECK)
{
    wxnotebook = new wxNotebook(this, wxID_ANY, wxPoint(APP_MARGIN, APP_MARGIN), wxSize(APP_WIDTH - 23, APP_HEIGHT - 45), wxNB_TOP);

    tab_status = new wxPanel(wxnotebook, wxID_ANY);
    tab_log = new wxPanel(wxnotebook, wxID_ANY);
    tab_settings = new wxPanel(wxnotebook, wxID_ANY);

    wxnotebook->AddPage(tab_status, "Status", true);
    wxnotebook->AddPage(tab_settings, "Settings");
    wxnotebook->AddPage(tab_log, "Log");

    #ifdef _WIN32
        this->SetIcon(icon);
    #endif

    tb = new wxMTaskbar(this);
    tb->SetIcon(icon, "Pocket Pad");
    
    InitStatusTab();
    InitSettingsTab();
    InitLogTab();
    
    // Start periodic device status checking (every 2 seconds)
    deviceCheckTimer.Start(2000);
}   

void wxMain::InitStatusTab()
{    
    wxBoxSizer *topsizer = new wxBoxSizer(wxVERTICAL);

    // ==== This PC info ====
    wxStaticBoxSizer *ipsizer = new wxStaticBoxSizer(wxVERTICAL, tab_status, "This PC");

    wxBoxSizer *r1 = new wxBoxSizer(wxHORIZONTAL);
    r1->Add(new wxStaticText(tab_status, wxID_ANY, "Name: "));
    wxStaticText *sb1 = new wxStaticText(tab_status, HOST_NAME_TXT, "");
    sb1->SetFont(wxFont(10, wxFONTFAMILY_DEFAULT, wxFONTSTYLE_NORMAL, wxFONTWEIGHT_BOLD));
    r1->Add(sb1);

    wxBoxSizer *r2 = new wxBoxSizer(wxHORIZONTAL);
    r2->Add(new wxStaticText(tab_status, wxID_ANY, "IP: "));
    wxStaticText *sb2 = new wxStaticText(tab_status, HOST_IP_TXT, "");
    sb2->SetFont(wxFont(10, wxFONTFAMILY_DEFAULT, wxFONTSTYLE_NORMAL, wxFONTWEIGHT_BOLD));
    r2->Add(sb2);

    ipsizer->Add(r1);
    ipsizer->Add(r2);

    // ==== Device status ====
    wxStaticBoxSizer *devsizer = new wxStaticBoxSizer(wxVERTICAL, tab_status, "Android Device");

    // Device status text
    deviceStatusText = new wxStaticText(tab_status, wxID_ANY, "Checking...");
    deviceStatusText->SetFont(wxFont(11, wxFONTFAMILY_DEFAULT, wxFONTSTYLE_NORMAL, wxFONTWEIGHT_BOLD));
    devsizer->Add(deviceStatusText, wxSizerFlags().Border(wxALL, 8));
    
    // Connection status text
    connectionStatusText = new wxStaticText(tab_status, wxID_ANY, "Waiting for connection...");
    connectionStatusText->SetFont(wxFont(10, wxFONTFAMILY_DEFAULT, wxFONTSTYLE_NORMAL, wxFONTWEIGHT_NORMAL));
    devsizer->Add(connectionStatusText, wxSizerFlags().Border(wxLEFT | wxRIGHT | wxBOTTOM, 8));

    // Device list
    wxdevlist = new wxDeviceList(tab_status, wxID_ANY);
    devsizer->Add(wxdevlist, wxSizerFlags(1).Expand().Border(wxALL, 5));

    topsizer->Add(ipsizer, wxSizerFlags().Expand().Border(wxALL, 5));
    topsizer->Add(devsizer, wxSizerFlags(1).Expand().Border(wxALL, 5));

    tab_status->SetSizerAndFit(topsizer);
}

void wxMain::InitLogTab()
{
    wxBoxSizer *topsizer3 = new wxBoxSizer(wxVERTICAL);
    
    wxListView *monitor = new wxListView(tab_log, wxID_ANY, wxDefaultPosition, wxDefaultSize, wxLC_REPORT);

    monitor->AppendColumn("Time");
    monitor->AppendColumn("Message", wxLIST_FORMAT_LEFT, 380);

    Logger::monitor = monitor;

    topsizer3->Add(monitor, wxSizerFlags(1).Expand());

    tab_log->SetSizerAndFit(topsizer3);
}

void wxMain::InitSettingsTab()
{
    wxBoxSizer *topsizer2 = new wxBoxSizer(wxVERTICAL);

    // ==== System settings ====
    wxStaticBoxSizer *sysboxsizer = new wxStaticBoxSizer(wxVERTICAL, tab_settings, "System");

    #ifdef _WIN32
        // Startup checkbox
        wxCheckBox *chk_startup = new wxCheckBox(tab_settings, CHK_STARTUP, "Run at startup");
        chk_startup->SetValue(settings.GetRunAtStartup());
        sysboxsizer->Add(chk_startup, wxSizerFlags().Border(wxTOP, 10));
    #endif

    // Minimize to taskbar checkbox
    wxCheckBox *chk_mintask = new wxCheckBox(tab_settings, CHK_MINTASK, "Minimize to taskbar");
    chk_mintask->SetValue(settings.GetMinToTaskbar());
    sysboxsizer->Add(chk_mintask, wxSizerFlags().Border(wxTOP | wxBOTTOM, 5));

    // ==== Controls settings ====
    wxStaticBoxSizer *ctrlboxsizer = new wxStaticBoxSizer(wxVERTICAL, tab_settings, "Controls");

    // Move sensitivity slider
    wxBoxSizer *b1 = new wxBoxSizer(wxHORIZONTAL);
    b1->Add(new wxStaticText(tab_settings, wxID_ANY, "Move sensitivity:"), wxSizerFlags().Border(wxTOP, 4));

    wxSlider *ms = new wxSlider(
        tab_settings, SL_MOVE, 10, 1, 20, wxDefaultPosition, wxSize(150, wxDefaultSize.y), 
        #ifdef _WIN32
            wxSL_TICKS 
        #else 
            wxSL_VALUE_LABEL
        #endif
    );

    ms->SetValue(settings.GetMoveSensitivity());
    b1->Add(ms);
    ctrlboxsizer->Add(b1);

    // Scroll sensitivity slider
    wxBoxSizer *b2 = new wxBoxSizer(wxHORIZONTAL);
    b2->Add(new wxStaticText(tab_settings, wxID_ANY, "Scroll sensitivity:"), wxSizerFlags().Border(wxTOP, 4));

    wxSlider *ss = new wxSlider(
        tab_settings, SL_SCROLL, 10, 1, 20, wxDefaultPosition, wxSize(150, wxDefaultSize.y), 
        #ifdef _WIN32
            wxSL_TICKS 
        #else 
            wxSL_VALUE_LABEL
        #endif
    );
    
    ss->SetValue(settings.GetScrollSensitivity());
    b2->Add(ss);
    ctrlboxsizer->Add(b2);

    // ==== Info ====
    wxBoxSizer *info = new wxBoxSizer(wxVERTICAL);

    wxStaticText *t = new wxStaticText(tab_settings, wxID_ANY, "Pocket Pad");
    t->SetFont(wxFont(10, wxFONTFAMILY_DEFAULT, wxFONTSTYLE_NORMAL, wxFONTWEIGHT_BOLD));
    info->Add(t, wxSizerFlags().Center());

    topsizer2->Add(sysboxsizer, wxSizerFlags().Expand().Border(wxALL, 5));
    topsizer2->Add(ctrlboxsizer, wxSizerFlags().Expand().Border(wxALL, 5));
    topsizer2->Add(info, wxSizerFlags().Expand().Border(wxTOP, 150));

    tab_settings->SetSizerAndFit(topsizer2);
}

void wxMain::SetHostInfo(std::string _Hostname, std::string _IpAddress)
{
    ((wxStaticText*)FindWindowById(HOST_NAME_TXT))->SetLabelText(_Hostname);
    ((wxStaticText*)FindWindowById(HOST_IP_TXT))->SetLabelText(_IpAddress);
}

void wxMain::HideWindow(wxIconizeEvent &evt)
{
    if(!settings.GetMinToTaskbar())
    {
        this->Hide();
        evt.Skip();
    }
}

void wxMain::CheckboxCmdHandler(wxCommandEvent &evt)
{
    if(evt.GetId() == CHK_STARTUP)
        settings.SetRunAtStartup(evt.IsChecked());
    else
        settings.SetMinimizeToTaskbar(evt.IsChecked());
}

void wxMain::SliderCmdHandler(wxScrollEvent &evt)
{
    if(evt.GetId() == SL_MOVE)
        settings.SetMoveSensitivity(evt.GetPosition());
    else
        settings.SetScrollSensitivity(evt.GetPosition());
}

void wxMain::OnDeviceCheckTimer(wxTimerEvent& evt)
{
    #ifdef _WIN32
        // Check device status
        adb::DeviceStatus status = settings.GetDeviceStatus();
        std::string message = settings.GetDeviceStatusMessage();
        
        deviceStatusText->SetLabelText(message);
        
        // Try to reconnect ADB if needed
        if (status == adb::DeviceStatus::CONNECTED || status == adb::DeviceStatus::MULTIPLE) {
            settings.TryReconnectADB();
        }
        
        // Update connection status based on ADB state and connected devices
        auto devices = server ? server->GetConnectedDevicesInfo() : std::vector<DeviceInfo>();
        if (!devices.empty()) {
            connectionStatusText->SetLabelText("Connected: " + devices[0].Name);
        } else if (settings.ADBState()) {
            connectionStatusText->SetLabelText("Ready - waiting for phone to connect...");
        } else if (status == adb::DeviceStatus::NONE) {
            connectionStatusText->SetLabelText("Connect your phone via USB");
        } else if (status == adb::DeviceStatus::UNAUTHORIZED) {
            connectionStatusText->SetLabelText("Authorize this computer on your phone");
        } else {
            connectionStatusText->SetLabelText("Waiting for connection...");
        }
    #endif
}

void wxMain::UpdateUI()
{
    // Initial status update
    #ifdef _WIN32
        adb::DeviceStatus status = settings.GetDeviceStatus();
        deviceStatusText->SetLabelText(settings.GetDeviceStatusMessage());
        
        if (status == adb::DeviceStatus::CONNECTED || status == adb::DeviceStatus::MULTIPLE) {
            settings.TryReconnectADB();
            connectionStatusText->SetLabelText("Ready - waiting for phone to connect...");
        } else if (status == adb::DeviceStatus::NONE) {
            connectionStatusText->SetLabelText("Connect your phone via USB");
        } else if (status == adb::DeviceStatus::UNAUTHORIZED) {
            connectionStatusText->SetLabelText("Authorize this computer on your phone");
        }
    #endif
}

wxMain::~wxMain()
{
    deviceCheckTimer.Stop();
    delete tb;
}
