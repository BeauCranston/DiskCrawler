import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class Controller {

    @FXML TextField directoryInput;
    @FXML TextField targetFile;
    @FXML Label filesFound;
    @FXML TextArea pathList;
    @FXML Button searchBtn;

    ArrayList<String> paths = new ArrayList<>();
    ProducerConsumer pc;

    /**
     * when the search button is clicked start the search task
     * @param e
     */
    public void search(MouseEvent e){
        System.out.println("search called");
        Thread t1 = new Thread(searchTask);
        //run the task in the background
        t1.setDaemon(true);
        t1.start();
    }

    /**
     * this task performs the search for the files.
     */
    Task searchTask = new Task() {
        @Override
        protected Void call() throws Exception {
            System.out.println("running task");
            String dirInput = directoryInput.getText();
            String fileInput = targetFile.getText();
            pc = new ProducerConsumer();
            pc.startIndexing(dirInput, fileInput);
            Platform.runLater(updateFileFound);
            return null;
        }
    };

    /**
     * this task updates the ui with the data received from the producer consumer
     */
    Task updateFileFound = new Task() {
        @Override
        protected Void call() throws Exception {
            int count  = pc.getMatchcount();
            filesFound.setText(String.valueOf(count));
            pathList.setText(pc.getMatches());
            return null;
        }
    };
}
