package com.example;

import com.aspose.pdf.Document;
import com.aspose.pdf.Page;
import com.aspose.pdf.TextFragment;
import com.aspose.pdf.TextFragmentAbsorber;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CouponGenerator1 extends Application {
    private static final Logger logger = Logger.getLogger(CouponGenerator1.class.getName());
    private static ProgressBar progressBar;
    private Stage primaryStage;
    private TextField searchTextTextField;
    private String templatePath;
    private String promoCodePath;
    private String outputPath;
    private Label progressLabel;

    public static void main(String[] args) {
        launch(args);
    }

    public static void update(String searchText, String templatePath, String promoCodePath, String outputPath) {
        try {
            Locale.setDefault(Locale.US);

            Document pdfDocument = new Document(templatePath);

            final String fileContent = readAndJoinPromoCodesFromFile(promoCodePath);

            final List<String> promoCodes = Arrays.stream(fileContent.split("\\n"))
                    .collect(Collectors.toList());

            final Iterator<String> promoCodeIterator = promoCodes.iterator();

            final Page templatePage = pdfDocument.getPages().get_Item(1);

            final TextFragmentAbsorber litle = new TextFragmentAbsorber(searchText);

            templatePage.accept(litle);

            int occurrenceCount = litle.getTextFragments().size();

            int totalPages = (int) Math.ceil((double) promoCodes.size() / occurrenceCount);

            final double[] progress = {0};

            for (int i = 1; i < totalPages; i++) {
                pdfDocument.getPages().add(templatePage);
                logger.info("Added template page to the document. Page number: " + i);
            }

            final int[] in = {0};


            Future<?> future = Executors.newWorkStealingPool(100).submit(() -> {
                for (Page page : pdfDocument.getPages()) {
                    TextFragmentAbsorber absorber = new TextFragmentAbsorber(searchText);
                    page.accept(absorber);
                    for (TextFragment textFragment : absorber.getTextFragments()) {
                        if (promoCodeIterator.hasNext()) {
                            textFragment.setText(promoCodeIterator.next());
                            logger.info("Added template page to the document. ADD TEXTS: " + in[0]++);
                            progress[0] += (1.0 / promoCodes.size());
                            double finalProgress = progress[0];
                            Platform.runLater(() -> progressBar.setProgress(finalProgress));
                        }
                    }
                }
            });

            future.get(1, TimeUnit.HOURS);

            pdfDocument.save(outputPath);
            pdfDocument.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Произошла ошибка: " + ex.getMessage());
        }
    }


    public static String readAndJoinPromoCodesFromFile(String filePath) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            return lines.collect(Collectors.joining("\n"));
        }
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("PDF Processing App");

        Label templateLabel = new Label("1. Select PDF Template:");
        Label promoCodesLabel = new Label("2. Select Promo Codes File:");
        Label outputFileLabel = new Label("4. Output File:");

        searchTextTextField = new TextField();
        searchTextTextField.setPromptText("3. Search word");

        Button templateBrowseButton = new Button("Browse");
        Button promoCodesBrowseButton = new Button("Browse");
        Button outputFileBrowseButton = new Button("Browse");
        Button processButton = new Button("5. Process PDF");

        progressBar = new ProgressBar(0);
        progressLabel = new Label("");

        templateBrowseButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                templatePath = selectedFile.getAbsolutePath();
                templateLabel.setText("1. Selected PDF Template: " + templatePath);
            }
        });

        promoCodesBrowseButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                promoCodePath = selectedFile.getAbsolutePath();
                promoCodesLabel.setText("2. Selected Promo Codes File: " + promoCodePath);
            }
        });

        outputFileBrowseButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File selectedFile = fileChooser.showSaveDialog(primaryStage);
            if (selectedFile != null) {
                outputPath = selectedFile.getAbsolutePath();
                outputFileLabel.setText("4. Output File: " + outputPath);
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, templateLabel, templateBrowseButton);
        grid.addRow(1, promoCodesLabel, promoCodesBrowseButton);
        grid.addRow(2, new Label("3. Search word:"), searchTextTextField);
        grid.addRow(3, outputFileLabel, outputFileBrowseButton);
        grid.add(processButton, 0, 4, 2, 1);
        grid.add(progressBar, 0, 5, 2, 1);
        grid.add(progressLabel, 0, 6, 2, 1);

        Scene scene = new Scene(grid, 800, 400);
        primaryStage.setScene(scene);

        primaryStage.centerOnScreen();

        primaryStage.show();

        processButton.setOnAction(event -> {
            String searchText = searchTextTextField.getText();

            Task<Void> pdfProcessingTask = new Task<>() {
                @Override
                protected Void call() {
                    // Вызываем метод обработки PDF
                    update(searchText, templatePath, promoCodePath, outputPath);
                    return null;
                }
            };

            pdfProcessingTask.setOnScheduled(e -> {
                progressBar.setProgress(0);
                progressLabel.setText("Processing...");
            });

            pdfProcessingTask.setOnSucceeded(e -> {
                progressBar.setProgress(1);
                progressLabel.setText("Processing completed.");
            });

            new Thread(pdfProcessingTask).start();
        });
    }
}
