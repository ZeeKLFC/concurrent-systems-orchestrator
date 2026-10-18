import java.util.List;
import java.util.ArrayList;

public class Tests {

        public static void main(String[] args) {
                Tests tests = new Tests();
                System.out.println("--- Running UR1 Test ---");
                tests.myUR1Test();

                try {
                        Thread.sleep(500);
                } catch (InterruptedException e) {
                }

                System.out.println("\n--- Running Example UR2 Test ---");
                tests.exampleUR2Test();

                tests.myUR3Test();

                tests.mySJFTest();

                tests.myUR4StressTest();

                tests.mySJFTieBreakTest();
        }

        public void myUR1Test() {
                JobShopManager jobShopManager = new JobShopManager("FCFS");
                String type = "FDM";

                for (int i = 1; i <= 5; i++) {
                        new MachineThread(jobShopManager, type, i).start();
                }

                try {
                        Thread.sleep(100);
                } catch (InterruptedException e) {
                }

                List<Operation> ops = new ArrayList<>();
                ops.add(new Operation(type, 1));
                ops.add(new Operation(type, 1));
                Job job1 = new Job("Job_UR1", ops);

                // Corrected: accessing public field directly
                System.out.println("Specifying " + job1.jobName + " (needs 2 " + type + "s)");
                jobShopManager.specifyJobs(java.util.Arrays.asList(job1));
        }

        public class MachineThread extends Thread {
                public final String machineType;
                public final int machineID;
                private final JobShopManager jobShopManager;

                public MachineThread(JobShopManager jobShopManager, String machineType, int machineID) {
                        this.jobShopManager = jobShopManager;
                        this.machineType = machineType;
                        this.machineID = machineID;
                        this.setName("Machine-" + machineType + "-" + machineID);
                }

                @Override
                public void run() {
                        String jobName = jobShopManager.thisMachineAvailable(machineType, machineID);
                        System.out.println(
                                        ">>> SUCCESS: " + machineType + " " + machineID + " released for " + jobName);
                }
        }

        public void exampleUR2Test() {
                JobShopManager jobShopManager = new JobShopManager("FCFS");

                System.out.println("Start the machines:");
                for (int i = 1; i <= 6; i++)
                        new MachineThread(jobShopManager, "FDM", i).start();
                for (int i = 1; i <= 2; i++)
                        new MachineThread(jobShopManager, "SLA", i).start();

                try {
                        Thread.sleep(100);
                } catch (InterruptedException e) {
                }

                Job job1 = new Job("Job1", java.util.Arrays.asList(
                                new Operation("FDM", 5),
                                new Operation("FDM", 3),
                                new Operation("FDM", 3),
                                new Operation("SLA", 3)));

                Job job2 = new Job("Job2", java.util.Arrays.asList(
                                new Operation("FDM", 5),
                                new Operation("FDM", 3)));

                System.out.println("Specifying Jobs...");
                jobShopManager.specifyJobs(java.util.Arrays.asList(job1, job2));

                try {
                        Thread.sleep(200);
                } catch (InterruptedException e) {
                }
        }

        public void myUR3Test() {
                JobShopManager manager = new JobShopManager("FCFS");
                System.out.println("\n--- Running UR3 Test (Jobs before Machines) ---");

                // 1. Specify jobs FIRST
                Job job1 = new Job("LateMachineJob", java.util.Arrays.asList(new Operation("CNC", 1)));
                manager.specifyJobs(java.util.Arrays.asList(job1));
                System.out.println("Job specified, waiting for machine...");

                // 2. Start machine LATER
                new MachineThread(manager, "CNC", 99).start();

        }
        
        public void mySJFTest() {
                System.out.println("\n--- Running UR6 Test (SJF Mode) ---");
                JobShopManager manager = new JobShopManager("SJF");

                // Long job (100 units)
                Job jobLong = new Job("LongJob", java.util.Arrays.asList(new Operation("FDM", 100)));
                // Short job (10 units)
                Job jobShort = new Job("ShortJob", java.util.Arrays.asList(new Operation("FDM", 10)));

                manager.specifyJobs(java.util.Arrays.asList(jobLong, jobShort));

                // I'll start one machine. It should pick the ShortJob first!
                new MachineThread(manager, "FDM", 777).start();
        }
    
        public void myUR4StressTest() {
                System.out.println("\n--- Running UR4 Stress Test (Interleaved Arrivals) ---");
                JobShopManager manager = new JobShopManager("FCFS");

                // I'll start one machine first, then a job that needs TWO machines, then the second machine.
                new MachineThread(manager, "DRILL", 1).start();

                try {
                        Thread.sleep(50);
                } catch (InterruptedException e) {
                }

                Job drillJob = new Job("DrillJob", java.util.Arrays.asList(
                                new Operation("DRILL", 10),
                                new Operation("DRILL", 10)));

                System.out.println("Specifying Job that needs 2 Drills...");
                manager.specifyJobs(java.util.Arrays.asList(drillJob));

                try {
                        Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                System.out.println("Starting second Drill machine...");
                new MachineThread(manager, "DRILL", 2).start();
        }
    
        public void mySJFTieBreakTest() {
        System.out.println("\n--- Running SJF Tie-Breaker Test ---");
        JobShopManager manager = new JobShopManager("SJF");
        
        // Two jobs with same time (10) but different names
        Job jobB = new Job("B_Job", java.util.Arrays.asList(new Operation("FDM", 10)));
        Job jobA = new Job("A_Job", java.util.Arrays.asList(new Operation("FDM", 10)));

        manager.specifyJobs(java.util.Arrays.asList(jobB, jobA));
        
        // Machine should get A_Job first because 'A' comes before 'B'
        new MachineThread(manager, "FDM", 101).start();
    }
}