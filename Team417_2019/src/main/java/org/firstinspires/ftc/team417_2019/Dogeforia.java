// This file came from DogeCV library from Wizards.exe
// package com.disnodeteam.dogecv;
// Removed Vuforia tracking because it's not used
// Need Vuforia to access the webcam

package org.firstinspires.ftc.team417_2019;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

import com.vuforia.Frame;

import org.corningrobotics.enderbots.endercv.DrawViewSource;
import org.firstinspires.ftc.robotcore.internal.vuforia.VuforiaLocalizerImpl;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.concurrent.BlockingQueue;

//import com.disnodeteam.dogecv.detectors.DogeCVDetector;

/**
 * An implementation of Vuforia intended to be cross-compatible with OpenCV (and DogeCV by extension)
 */

public class Dogeforia extends VuforiaLocalizerImpl
{
    private OpenCVDetect OpenCV_detector;

    DrawViewSource displayView;
    boolean dogeCVEnabled;
    boolean showDebug = false;

    Thread workerThread;
    Bitmap outputImage;
    Bitmap bitmap;
    Bitmap rotatedBitmap;
    Mat inputMat;
    Mat outMat;
    BlockingQueue<CloseableFrame> frames;
    public Dogeforia(Parameters parameters) {
        super(parameters);
    }

    public void setDogeCVDetector(OpenCVDetect OpenCV_detector){
        this.OpenCV_detector = OpenCV_detector;
        OpenCV_detector.enable();
        displayView = OpenCV_detector.getRawView();
        setMonitorViewParent(displayView.getId());
        setFrameQueueCapacity(1);
    }

    public void start(){
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!workerThread.isInterrupted()){
                    render();
                }
            }
        });
        workerThread.setName("Dogeforia Thread");
        workerThread.start();

        Log.d("DogeCV", workerThread.getState().toString());

    }

    public void enableDogeCV(){

        dogeCVEnabled = true;
    }

    public void disableDogeCV(){
        dogeCVEnabled = false;
    }
    public void enableTrack(){
        startTracker();
    }

    public void disableTrack() {
        stopTracker();
    }
    public void showDebug(){
        showDebug = true;

    }

    public void processFrame(Frame frame){
        if(frame != null ){

            bitmap = convertFrameToBitmap(frame);

            inputMat = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
            Utils.bitmapToMat(bitmap,inputMat);

            outMat = OpenCV_detector.processFrame(inputMat, null);

            if(!outMat.empty() ){
                // should not need this since outMat is same dim as inputMat
                //bitmap.setHeight(outMat.height());
                //bitmap.setWidth(outMat.width());

                Mat rotatedMat;
                rotatedMat = new Mat(outMat.height(), outMat.width(), CvType.CV_8UC1);
                Core.rotate(outMat, rotatedMat, Core.ROTATE_90_CLOCKWISE);

                // create bitmap, swap height, width for rotation
                rotatedBitmap = Bitmap.createBitmap(rotatedMat.width(), rotatedMat.height(), Bitmap.Config.RGB_565);
                rotatedBitmap.setHeight(rotatedMat.height());
                rotatedBitmap.setWidth(rotatedMat.width());
                Utils.matToBitmap(rotatedMat, rotatedBitmap);
                //Utils.matToBitmap(rotatedMat, bitmap);

                // adjusted height = <user-chosen width> * original height / original width
                int dispWidth = displayView.getWidth();      //656
                int dispHeight = displayView.getHeight();    // 1054
                int rotatedMatHeight = rotatedMat.height();  //800
                int rotatedMatWidth = rotatedMat.width();    // 448
                double adjustedWidth = rotatedMatWidth * dispHeight / dispWidth;  // 367
//                outputImage =  Bitmap.createScaledBitmap(rotatedBitmap, (int)adjustedWidth, dispWidth, false);
                outputImage =  Bitmap.createScaledBitmap(rotatedBitmap, (int)adjustedWidth, dispHeight, false);

                ((Activity)displayView.getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayView.onFrame(outputImage);
//                        displayView.onFrame(rotatedBitmap);
                        displayView.invalidate();
                    }
                });

            }else{
                Log.w("DogeCV", "MAT BITMAP MISMATCH OR EMPTY ERROR");
            }


            inputMat.release();
            outMat.release();


        }else{
            Log.d("DogeCV", "No Frame!");
        }
    }

    public void render() {
       // Log.d("DogeCV", "Rendering Frame");
       // super.onRenderFrame()

        if(OpenCV_detector != null && dogeCVEnabled){

            if(!getFrameQueue().isEmpty()){
                try {
                    processFrame(getFrameQueue().take());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{
                // the following line floods the log with messages; remove it
                //Log.w("DogeCV", "Frame is empty. Enabling AparnaCV: " + getFrameQueueCapacity());
            }

            /*
            getFrameOnce(Continuation.create(ThreadPool.getDefault(), new Consumer<Frame>()
            {
                @Override public void accept(Frame frame)
                {
                    processFrame(frame);
                }
            }));
             */
        }

    }

    public void stop(){
        close();
        ((Activity)displayView.getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                workerThread.interrupt();

                OpenCV_detector.disable();
            }
        });

    }
}
