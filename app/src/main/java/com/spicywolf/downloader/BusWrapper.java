package com.spicywolf.downloader;


import com.squareup.otto.Bus;

public class BusWrapper {

    private static Bus sBus = new Bus();

    public static Bus getBus() {
        return sBus;
    }
}
