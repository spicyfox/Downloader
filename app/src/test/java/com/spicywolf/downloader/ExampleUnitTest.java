package com.spicywolf.downloader;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);

        try {
            //List<XmlParser.Entry> ret = new XmlParser().parse(getResources().getAssets().open("economy.xml"));
            List<XmlParser.Entry> ret = new XmlParser().parse(getResources().getAssets().open("iamprogrammer.xml"));
            if (ret != null) {
                for(XmlParser.Entry cur : ret) {
                    Log.i(TAG, cur.toString());
                }
            }

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}