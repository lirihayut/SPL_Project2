package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StatisticalFolder;

public class TimeService extends MicroService {
    private int duration;
    private final int speed;
    private int currentTime;
    private boolean serviceExist;

    public TimeService(int tickTime, int duration) {
        super("TimeService - TickTime: " + tickTime + ", Duration: " + duration);
        this.duration = duration;
        this.speed = tickTime;
        this.currentTime = 0;
        this.serviceExist=true;
    }

    @Override
    protected void initialize() {
        subscribeBroadcast(TerminatedBroadcast.class, terminated -> {
            if(terminated.getMessage() == "FusionSlamService") {
                serviceExist = false;
            }
        });



        StatisticalFolder statisticalFolder = StatisticalFolder.getInstance();
        if (statisticalFolder == null) {
            throw new IllegalStateException("StatisticalFolder instance is null");
        }
        Thread tickThread = new Thread(() -> {
            try {
                while (serviceExist && currentTime < duration) {
                    Thread.sleep(100);
                    System.out.println("TimeService: Current Tick = " + currentTime);
                    sendBroadcast(new TickBroadcast(currentTime));
                    currentTime++;
                    statisticalFolder.incrementSystemRuntime();
                }
                sendBroadcast(new TerminatedBroadcast("TimeService"));
                terminate();
                System.out.println("TimeService has terminated...");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("TimeService thread interrupted: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("TimeService encountered an error: " + e.getMessage());
            }
        });
        tickThread.start();
    }
}