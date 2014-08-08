import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.Style;

public class TimeMachineStatus
{
    private JFrame mainFrame;
    private JLabel headerLabel;
    private JLabel statusLabel;
    private JPanel controlPanel;
    private JLabel msglabel;
    private JProgressBar progressBar;
    private int timeDelay = 4000;

    public TimeMachineStatus()
    {
        prepareGUI();
    }

    public static void main(String[] args){
        TimeMachineStatus  tmstatus = new TimeMachineStatus();
        tmstatus.checkStatus();
    }

    private void prepareGUI()
    {
        JFrame f = new JFrame("Time Machine Status");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container content = f.getContentPane();
        progressBar = new JProgressBar();
        progressBar.setValue(progressBar.getMinimum());
        progressBar.setStringPainted(true);
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d 'at' HH:mm");
        Border border = BorderFactory.createTitledBorder("Backup started on " + sdf.format(cal.getTime()));
        statusLabel = new JLabel("Status: ",JLabel.LEFT);
        progressBar.setBorder(border);
        content.add(progressBar, BorderLayout.NORTH);
        //content.add(statusLabel, BorderLayout.SOUTH);
        f.setUndecorated(true);
        f.setSize(270, 50);
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        final int WIDTH = screenSize.width;
        final int HEIGHT = screenSize.height;
        f.setLocation(1, HEIGHT - 50);
        f.setAlwaysOnTop (true);
        f.setVisible(true);
    }

    private void checkStatus()
    {
        int percent = 0;

        //check if backup is running
        String output = runCommand("tmutil status");
        //String output = runCommand("cat /Users/stealth/tm4");
        int running = output.indexOf("Running =");
        int isRunning = Integer.valueOf(output.substring(running + 10, running + 11));

        if (isRunning == 1)
        {
            while(percent < 100)
            {
                try {
                    Thread.sleep(timeDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                String result = runCommand("tmutil status");
                //String result = runCommand("cat /Users/stealth/tm2");
                String[] outputArray = result.split(";");

                // get status
                String toPrint = "";
                for (String str : outputArray) {
                    int backupPhaseInd = str.indexOf("BackupPhase =");
                    if (backupPhaseInd > -1) {
                        String backupPhase = str.substring(backupPhaseInd + 14);
                        //progressBar.setString(backupPhase);
                        //System.out.println(backupPhase);
                        if (backupPhase.compareTo("ThinningPreBackup") == 0) {
                            toPrint += "Preparing Backup";
                        }
                        if (backupPhase.compareTo("Starting") == 0) {
                            toPrint += "Starting";
                        }
                        break;
                    }
                }


                for (String str : outputArray) {
                    int id = str.indexOf("bytes =");
                    if (id > -1) {
                        String bytes = str.substring(id + 8);
                        double inBytes = Double.valueOf(bytes);
                        if (inBytes / (1000000) > 1000) {
                            toPrint += ((int)(Double.valueOf(bytes) / 100000000.0)) / 10.0 + "GB of ";
                        } else {
                            toPrint += ((int)(Double.valueOf(bytes) / 100000.0)) / 10.0 + "MB of ";
                        }

                        break;
                    }
                }

                for (String str : outputArray) {
                    int id = str.indexOf("totalBytes =");
                    if (id > -1) {
                        String bytes = str.substring(id + 12);
                        double inBytes = Double.valueOf(bytes);
                        if (inBytes / (1000000) > 1000) {
                            toPrint += ((int)(Double.valueOf(bytes) / 100000000.0)) / 10.0 + "GB";
                        } else {
                            toPrint += ((int)(Double.valueOf(bytes) / 100000.0)) / 10.0 + "MB";
                        }
                        break;
                    }
                }
                progressBar.setString(toPrint);

                // get percentage
                for (String str : outputArray) {
                    int backupPercentInd = str.indexOf("Percent =");
                    if (backupPercentInd > -1) {

                        String backupPercent = str.substring(backupPercentInd + 9);
                        if (str.endsWith("\"")) {
                            percent = ((int) (Double.valueOf(backupPercent.substring(2, backupPercent.length() - 1)) * 100));
                        } else {
                            percent = ((int) (Double.valueOf(backupPercent.substring(1, backupPercent.length() )) * 100));
                        }
                        progressBar.setValue(percent);
                        break;
                    }
                }

            }
        }

        output = runCommand("tmutil status");
        running = output.indexOf("Running =");
        isRunning = Integer.valueOf(output.substring(running + 10, running + 11));

        while (isRunning == 1) {

            try {
                Thread.sleep(timeDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // get status of TM
            String result = runCommand("tmutil status");
            //String result = runCommand("cat /Users/stealth/tm2");
            String[] outputArray = result.split(";");

            for (String str : outputArray) {
                int backupPhaseInd = str.indexOf("BackupPhase =");
                if (backupPhaseInd > -1) {
                    String backupPhase = str.substring(backupPhaseInd + 14);
                    if (backupPhase.compareTo("ThinningPostBackup") == 0) {
                        progressBar.setString("Cleaning Up");
                    }
                    if (backupPhase.compareTo("Finishing") == 0) {
                        progressBar.setString("Finishing");
                    }
                    progressBar.setString(backupPhase);
                    break;
                }
            }

            running = result.indexOf("Running =");
            isRunning = Integer.valueOf(result.substring(running + 10, running + 11));
        }



        // finish and close the program
        try {
            Thread.sleep(timeDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private String runCommand(String cmd) {
        String output = "";
        try {

            Process perkele = Runtime.getRuntime().exec(cmd);
            BufferedReader buf = new BufferedReader(new InputStreamReader(perkele.getInputStream()));
            String line = "";
            while ((line = buf.readLine()) != null) {
                output += line;
            }
        } catch (IOException e) {
            System.out.print(e.toString());
        }
        return output;
    }
}