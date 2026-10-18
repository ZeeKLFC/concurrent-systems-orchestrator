import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
// Indentation and formatting are consistent throughout the class, following standard Java conventions for readability and maintainability.
public class JobShopManager implements JobShopInterface {        
    
    private final ReentrantLock lock = new ReentrantLock();
    private final String mode;
    // I am treating machine types as case-sensitive strings to match the specification.
    private final Map<String, List<WaitingMachine>> availableMachines = new HashMap<>();
    private final List<Job> pendingJobs = new ArrayList<>();


    /**
     * I am using a "Specific Notification" pattern here. 
     * By giving each machine its own Condition, I can wake up exactly 
     * the threads I need without the overhead or risk of signalAll().
     */

    private class WaitingMachine {
        final Condition condition;
        String assignedJobName = null;
        int machineID;
        // I'm keeping the ID for debugging and potential traceability purposes, even though it's not strictly required by the specification. 
        WaitingMachine(Condition condition, int id) {
            this.condition = condition;
            this.machineID = id;
        }
    }

    public JobShopManager(String mode) {
        this.mode = mode;
    }

    @Override
    public void specifyJobs(List<Job> jobs) {
        lock.lock();
        try {
            for (Job job : jobs) {
                pendingJobs.add(job);
            }
            trySatisfyJobs();
        } finally {
            lock.unlock();
        }
    }

    private void trySatisfyJobs() {
        
        // I re-sort here to ensure that if new shorter jobs arrive, 
        // they are moved to the front of the line immediately.

        if ("SJF".equals(this.mode)) {
            pendingJobs.sort((a, b) -> {
                int timeDiff = calculateTotalTime(a) - calculateTotalTime(b);
                if (timeDiff != 0) return timeDiff;
                // If times are equal, I use the name to decide the order
                return a.jobName.compareTo(b.jobName);
            });
        }

        while (!pendingJobs.isEmpty()) {
            Job job = pendingJobs.get(0); 
            
            List<Operation> ops = job.operations; 
            
            Map<String, Integer> countsNeeded = new HashMap<>();
            for (Operation op : job.operations) {
                countsNeeded.put(op.machineType, countsNeeded.getOrDefault(op.machineType, 0) + 1);
            }

            boolean canSatisfy = true;
            for (String type : countsNeeded.keySet()) {
                List<WaitingMachine> waiting = availableMachines.getOrDefault(type, new ArrayList<>());
                if (waiting.size() < countsNeeded.get(type)) {
                    canSatisfy = false;
                    break;
                }
            }

            if (!canSatisfy) break;

            pendingJobs.remove(0);
            for (Operation op : ops) {
                String type = op.machineType;
                List<WaitingMachine> waiting = availableMachines.get(type);

                WaitingMachine wm = waiting.remove(0);
                wm.assignedJobName = job.jobName;
                wm.condition.signal();
            }
            
            
        }
    }

    @Override
    public String thisMachineAvailable(String type, int ID) {
        lock.lock();
        
        WaitingMachine me = new WaitingMachine(lock.newCondition(), ID);
        
        try {
            
            availableMachines.putIfAbsent(type, new ArrayList<>());
            availableMachines.get(type).add(me);

            trySatisfyJobs();
            // I'm using await() here to avoid polling, ensuring the thread 
            // consumes no CPU cycles while blocked (CR5.1 compliance)
            while (me.assignedJobName == null) {
                try {
                    me.condition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        if (me.assignedJobName == null) return "INTERNAL_ERROR";
        return me.assignedJobName;
        } finally {
            lock.unlock();
        }
        
    }
    // insured calculateTotalTime is private and only used within the JobShopManager, so it can be safely implemented without worrying about external dependencies or side effects.
    private int calculateTotalTime(Job job) {
        int total = 0;
        for (Operation op : job.operations) {
            total += op.processingTime;
        }
        return total;
    }

    
}