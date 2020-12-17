package com.poutinenerds.happinging;
// This interface is needed to return the values from the Async Task  running the bandwidth test

public interface OnDataSendToActivity {
    public void sendData(Double str);
}
