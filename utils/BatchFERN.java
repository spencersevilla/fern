import java.io.*;
import com.spencersevilla.fern.FERNInterface;
import com.spencersevilla.fern.CommandLineParser;

public class BatchFERN {

    public static void main(String[] args) throws Exception {
		FERNInterface fern = new FERNInterface();

        if (args.length != 1) {
            System.out.println("usage: BatchFERN [path/to/conf_file]");
            return;
        }

        String file = args[0];

        FileInputStream fs = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fs);
        BufferedReader br = new BufferedReader(isr);

        String line = null;
        while ((line = br.readLine()) != null) {
            if (!line.equals("")) {
                CommandLineParser.readCommandLine(line, fern);                  
            }
        }

        fs.close();
        return;
    }
}