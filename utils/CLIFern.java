import java.io.*;
import com.spencersevilla.fern.FERNInterface;
import com.spencersevilla.fern.CommandLineParser;

public class CLIFern {

    public static void main(String[] args) throws Exception {
    	FERNInterface fern = null;
    	try {
			fern = new FERNInterface();    		
    	} catch (Exception e) {
    		e.printStackTrace();
    		return;
    	}

		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		String command = "#";


    	do {
            CommandLineParser.readCommandLine(command, fern);

	    	System.out.println("Enter a command:");
	    	command = br.readLine();
    	} while (!command.equals("quit") && !command.equals("exit"));
    }
}
