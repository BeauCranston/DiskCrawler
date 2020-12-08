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

    public void search(MouseEvent e){


    }


    Task searchTask = new Task() {
        @Override
        protected Object call() throws Exception {
            String dirInput = directoryInput.getText();
            String fileInput = targetFile.getText();
            ProducerConsumer.startIndexing(dirInput, fileInput);
        }
    }
}
