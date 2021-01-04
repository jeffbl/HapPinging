package com.poutinenerds.happinging;
// This interface is needed to return the values from the Async Task  running the bandwidth test

import android.content.Intent;


public interface OnDataSendToActivity {
    public void sendData(Double str);

}
