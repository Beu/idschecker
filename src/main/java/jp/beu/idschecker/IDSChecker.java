package jp.beu.idschecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;


/**
 * IDS Checker with JavaFX GUI
 */
public class IDSChecker extends Application {

	/**
	 * entry point
	 * @param arguments are command line parameters, but ignored
	 */
	public static void main(String[] arguments) {
		Application.launch(arguments);
	}

	private Stage stage;

	@Override
	public void start(Stage primaryStage) throws Exception {
		System.out.println("start");
		// (maybe) load fonts into the VM
		{
			Font.loadFont(IDSChecker.class.getClassLoader().getResourceAsStream("fonts/HanaMinA.ttf"), 24.0d);
			Font.loadFont(IDSChecker.class.getClassLoader().getResourceAsStream("fonts/HanaMinB.ttf"), 24.0d);
			Font.loadFont(IDSChecker.class.getClassLoader().getResourceAsStream("fonts/NotoSerifCJKjp-Regular.otf"), 24.0d);
			Font.loadFont(IDSChecker.class.getClassLoader().getResourceAsStream("fonts/NotoSerifCJKkr-Regular.otf"), 24.0d);
			Font.loadFont(IDSChecker.class.getClassLoader().getResourceAsStream("fonts/NotoSerifCJKsc-Regular.otf"), 24.0d);
			Font.loadFont(IDSChecker.class.getClassLoader().getResourceAsStream("fonts/NotoSerifCJKtc-Regular.otf"), 24.0d);
		}
		stage = primaryStage;
		stage.setTitle("IDSChecker");
		stage.setWidth(800.0d);
		stage.setHeight(600.0d);
		{
			Scene scene = new Scene(createMainWindow());
			stage.setScene(scene);
		}
		stage.show();
	}

	@Override
	public void stop() throws Exception {
		stage.close();
		System.out.println("stop");
	}

	private Parent createMainWindow() {
		BorderPane borderPane = new BorderPane();
		{
			MenuBar menuBar = createMenuBar();
			borderPane.setTop(menuBar);
		}
		{
			Pane mainPane = createMainPane();
			borderPane.setCenter(mainPane);
		}
		return borderPane;
	}

	private MenuBar createMenuBar() {
		MenuBar menuBar = new MenuBar();
		{
			Menu fileMenu = createFileMenu();
			Menu checkMenu = createCheckMenu();
			Menu editMenu = createEditMenu();
			Menu helpMenu = createHelpMenu();
			menuBar.getMenus().addAll(fileMenu, checkMenu, editMenu, helpMenu);
		}
		return menuBar;
	}

	private File IDS_DIR = null;

	private MenuItem check1MenuItem = new MenuItem("Check1");
	private MenuItem check2MenuItem = new MenuItem("Check2");
	private MenuItem check3MenuItem = new MenuItem("Check3");
	private MenuItem check4MenuItem = new MenuItem("Check4");

	private Menu createFileMenu() {
		Menu menu = new Menu("File");
		{
			MenuItem item = new MenuItem("Select IDS Directory...");
			item.setOnAction((ActionEvent) -> {
				DirectoryChooser dirChooser = new DirectoryChooser();
				dirChooser.setTitle("Select IDS Directory...");
				IDS_DIR = dirChooser.showDialog(stage);
				if (IDS_DIR != null) {
					// check simply
					File[] files = IDS_DIR.listFiles(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return name.startsWith("IDS-") && name.endsWith(".txt");
						}
					});
					if (files == null || files.length == 0) {
						// this directory is not for IDS
						IDS_DIR = null;
					}
				}
				if (IDS_DIR == null) {
					stage.setTitle("IDSCheck");
					check1MenuItem.setDisable(true);
					check2MenuItem.setDisable(true);
					check3MenuItem.setDisable(true);
					check4MenuItem.setDisable(true);
				} else {
					stage.setTitle("IDSCheck - " + IDS_DIR.getPath());
					check1MenuItem.setDisable(false);
					check2MenuItem.setDisable(false);
					check3MenuItem.setDisable(false);
					check4MenuItem.setDisable(false);
				}
			});
			menu.getItems().add(item);
		}
		menu.getItems().add(new SeparatorMenuItem());
		{
			MenuItem item = new MenuItem("Quit...");
			item.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.ALT_DOWN));
			item.setOnAction((ActionEvent event) -> {
				Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure?", ButtonType.YES, ButtonType.NO);
				alert.setTitle("confirm quitting");
				alert.setHeaderText("You will quit this application.");
				alert.showAndWait()
						.filter((ButtonType buttonType) -> buttonType == ButtonType.YES)
						.ifPresent((ButtonType buttonType) -> {
							Platform.exit();
						});
			});
			menu.getItems().add(item);
		}
		return menu;
	}

	private Menu createCheckMenu() {
		Menu menu = new Menu("Check");
		{
			check1MenuItem.setOnAction((ActionEvent event) -> {
				try {
					new Check1Stage(IDS_DIR);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			check1MenuItem.setDisable(true);
			menu.getItems().add(check1MenuItem);
		}
		{
			check2MenuItem.setOnAction((ActionEvent event) -> {
				try {
					new Check2Stage(IDS_DIR);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			check2MenuItem.setDisable(true);
			menu.getItems().add(check2MenuItem);
		}
		{
			check3MenuItem.setOnAction((ActionEvent event) -> {
				try {
					new Check3Stage(IDS_DIR);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			check3MenuItem.setDisable(true);
			menu.getItems().add(check3MenuItem);
		}
		{
			check4MenuItem.setOnAction((ActionEvent event) -> {
				try {
					new Check4Stage(IDS_DIR);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			check4MenuItem.setDisable(true);
			menu.getItems().add(check4MenuItem);
		}
		return menu;
	}

	private Menu createEditMenu() {
		Menu menu = new Menu("Edit");
		{
			MenuItem item = new MenuItem("Unicode Utilities...");
			item.setOnAction((ActionEvent event) -> {
				final Dialog<Void> dialog = new Dialog<>();
				dialog.setTitle("Unicode Utilities");
				{
					DialogPane dialogPane = new DialogPane();
					{
						VBox vBox = new VBox();
						vBox.setSpacing(8.0d);
						{
							Label label = new Label("Character to Unicode code-point");
							vBox.getChildren().add(label);
						}
						TextField inputTextField = new TextField();
						TextField outputTextField = new TextField();
						{
							HBox hBox = new HBox();
							hBox.setSpacing(8.0d);
							{
								inputTextField.setStyle("-fx-font-size:200%; font-family:'Noto Serif CJK JP',HanaMinA,HanaMinB;");
								inputTextField.setPromptText("paste a character");
								hBox.getChildren().add(inputTextField);
							}
							{
								Button button = new Button("Convert");
								button.setOnAction((ActionEvent event2) -> {
									outputTextField.setText(
											inputTextField.getText().codePoints()
													.mapToObj((int codePoint) -> String.format("U+%04X (%s)", codePoint, BlockInfo.getBlockInfo(codePoint).name))
													.collect(Collectors.joining(" ")));
								});
								hBox.getChildren().add(button);
							}
							vBox.getChildren().add(hBox);
						}
						{
							outputTextField.setEditable(false);
							vBox.getChildren().add(outputTextField);
						}
						vBox.getChildren().add(new Separator());
						dialogPane.setContent(vBox);
					}
					dialog.setDialogPane(dialogPane);
					{
						ButtonType buttonType = new ButtonType("Close", ButtonData.CANCEL_CLOSE);
						dialog.getDialogPane().getButtonTypes().add(buttonType);
					}
					dialog.initModality(Modality.NONE);
					dialog.show();
				}
			});
			menu.getItems().add(item);
		}
		return menu;
	}

	private Menu createHelpMenu() {
		Menu menu = new Menu("Help");
		{
			MenuItem item = new MenuItem("About...");
			item.setOnAction((ActionEvent event) -> {
				Alert alert = new Alert(AlertType.INFORMATION, null, ButtonType.OK);
				alert.setTitle("About...");
				alert.setHeaderText("IDSChecker3");
				alert.showAndWait();
			});
			menu.getItems().add(item);
		}
		return menu;
	}

	private Pane createMainPane() {
		TextFlow textFlow = new TextFlow();
		textFlow.setPadding(new Insets(8.0d));
		textFlow.setTextAlignment(TextAlignment.LEFT);
		{
			Text text = new Text(
					"(1) Select IDS directory with File menu.\n\n"
					+ "(2) Select any check item with Check menu.\n");
			text.setStyle("-fx-font-family:monospace; -fx-font-size:200%;");
			textFlow.getChildren().add(text);
		}
		return textFlow;
	}
}

////////////////////////////////
//CheckBase
////////////////////////////////

abstract class CheckBaseStage extends Stage {

	protected File IDS_DIR;
	protected static List<BlockInfo> blockInfoList = BlockInfo.getBlockInfoList();

	public CheckBaseStage(File IDS_DIR) throws IOException {
		this.IDS_DIR = IDS_DIR;
		this.setWidth(800.0d);
		this.setHeight(600.0d);
		{
			Scene scene = new Scene(createMainWindow());
			this.setScene(scene);
		}
//		this.show();  // WebView が 一旦 HTML を 讀み取りて後
	}

	private Parent createMainWindow() throws IOException {
		BorderPane borderPane = new BorderPane();
		{
			MenuBar menuBar = createMenuBar();
			borderPane.setTop(menuBar);
		}
		{
			Parent mainPane = createMainPane();
			borderPane.setCenter(mainPane);
		}
		return borderPane;
	}

	private MenuBar createMenuBar() {
		MenuBar menuBar = new MenuBar();
		{
			Menu fileMenu = createFileMenu();
			Menu viewMenu = createViewMenu();
			Menu editMenu = createEditMenu();
			menuBar.getMenus().addAll(fileMenu, viewMenu, editMenu);
		}
		return menuBar;
	}

	protected WebView webView;
	protected WebEngine webEngine;

	private Menu createFileMenu() {
		Menu menu = new Menu("File");
		{
			MenuItem item = new MenuItem("Save As HTML...");
			item.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.ALT_DOWN));
			item.setOnAction((ActionEvent event) -> {
				Document document = webEngine.getDocument();
				FileChooser chooser = new FileChooser();
				File file = chooser.showSaveDialog(null);
				if (file != null) {
					try (FileOutputStream fos = new FileOutputStream(file)) {
						serializeDocument(document, fos);
					} catch (IllegalAccessException | InstantiationException | ClassNotFoundException | IOException | TransformerException e) {
						e.printStackTrace();
					}
				}
			});
			menu.getItems().add(item);
		}
		{
			MenuItem item = new MenuItem("Quit...");
			item.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.ALT_DOWN));
			item.setOnAction((ActionEvent event) -> {
				Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure?", ButtonType.YES, ButtonType.NO);
				alert.setTitle("Confirm Quitting");
				alert.showAndWait()
						.filter((ButtonType buttonType) -> buttonType == ButtonType.YES)
						.ifPresent((ButtonType buttonType) -> {
							CheckBaseStage.this.close();
						});
			});
			menu.getItems().add(item);
		}
		return menu;
	}

	private ToggleGroup fontToggleGroup = new ToggleGroup();

	private Menu createViewMenu() {
		Menu menu = new Menu("View");
		{
			MenuItem item = new MenuItem("Zoom In");
			item.setOnAction((ActionEvent) -> {
				Platform.runLater(() -> {
					webEngine.executeScript("zoomIn();");
				});
			});
			menu.getItems().add(item);
		}
		{
			MenuItem item = new MenuItem("Zoom Reset");
			item.setOnAction((ActionEvent event) -> {
				Platform.runLater(() -> {
					webEngine.executeScript("zoomReset();");
				});
			});
			menu.getItems().add(item);
		}
		{
			MenuItem item = new MenuItem("Zoom Out");
			item.setOnAction((ActionEvent event) -> {
				Platform.runLater(() -> {
					webEngine.executeScript("zoomOut();");
				});
			});
			menu.getItems().add(item);
		}
		{
			Menu fontMenu = new Menu("Select Font...");
			fontToggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
				@Override
				public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
					CheckBaseStage.this.check();
				}
			});
			{
				RadioMenuItem tcItem = new RadioMenuItem("繁");
				tcItem.setUserData("Noto Serif CJK TC");
				tcItem.setToggleGroup(fontToggleGroup);
				RadioMenuItem scItem = new RadioMenuItem("简");
				scItem.setUserData("Noto Serif CJK SC");
				scItem.setToggleGroup(fontToggleGroup);
				RadioMenuItem jpItem = new RadioMenuItem("日");
				jpItem.setUserData("Noto Serif CJK JP");
				jpItem.setToggleGroup(fontToggleGroup);
				RadioMenuItem krItem = new RadioMenuItem("韓");
				krItem.setUserData("Noto Serif CJK KR");
				krItem.setToggleGroup(fontToggleGroup);
				fontMenu.getItems().addAll(tcItem, scItem, jpItem, krItem);
				// set default
				jpItem.setSelected(true);
			}
			menu.getItems().add(fontMenu);
		}
		return menu;
	}

	private Menu createEditMenu() {
		Menu menu = new Menu("Edit");
		{
			MenuItem item = new MenuItem("Find...");
			item.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.ALT_DOWN));
			item.setOnAction((ActionEvent event) -> {
				Dialog<Void> dialog = new Dialog<>();
				dialog.setTitle("Find...");
				{
					DialogPane dialogPane = new DialogPane();
					{
						VBox vBox = new VBox();
						vBox.setSpacing(8.0d);
						{
							HBox hBox = new HBox();
							hBox.setSpacing(8.0d);
							final TextField inputTextField = new TextField();
							inputTextField.setStyle(String.format("-fx-font-size:150%%; -fx-font-family:'%s',HanaMinA,HanaMinB;", fontToggleGroup.getSelectedToggle().getUserData()));
							{
								hBox.getChildren().add(inputTextField);
							}
							{
								Button button = new Button("Find");
								button.setDefaultButton(true);
								button.setOnAction((ActionEvent) -> {
									Platform.runLater(() -> {
										findText(inputTextField.getText());
									});
								});
								hBox.getChildren().add(button);
							}
							vBox.getChildren().add(hBox);
						}
						dialogPane.setContent(vBox);
					}
					{
						ButtonType buttonType = new ButtonType("Close", ButtonData.CANCEL_CLOSE);
						dialogPane.getButtonTypes().add(buttonType);
					}
					dialog.setDialogPane(dialogPane);
				}
				dialog.initModality(Modality.WINDOW_MODAL);
				dialog.show();
			});
			menu.getItems().add(item);
		}
		return menu;
	}

	private Parent createMainPane() {
		webView = new WebView();
		webEngine = webView.getEngine();
		webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
			@Override
			public void changed(ObservableValue ov, State oldState, State newState) {
				if (newState == State.SUCCEEDED) {
					System.out.println("loaded");
					CheckBaseStage.this.show();
//					Platform.runLater(() -> {
//						CheckBaseStage.this.check();
//					});
					new Thread(() -> {
						CheckBaseStage.this.check();
					}).start();
				}
			}
		});
		try (InputStream is = IDSChecker.class.getClassLoader().getResourceAsStream("initial.xhtml");
				InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
				BufferedReader br = new BufferedReader(isr)) {
			String content = br.lines().collect(Collectors.joining("\n"));
			webEngine.loadContent(content, "application/xhtml+xml");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return webView;
	}

	public abstract void check();

	protected void clearMain() {
		String script = "clearMain();";
		webEngine.executeScript(script);
	}

	protected void printLine(String line) {
		String script = String.format("printLine(\"%s\");", line.replaceAll("\"", "\\\""));
		webEngine.executeScript(script);
	}

	protected void printStyledLine(String line, String style) {
		line = line.replaceAll("\"",  "\\\"");
		style = style.replaceAll("\"", "\\\"");
		String script = String.format("printStyledLine(\"%s\", \"%s\");", line, style);
		webEngine.executeScript(script);
	}

	protected void printAutoStyledLine(String line) {
		Pair<String[], String[]> pair = createStyledTexts(line);
		printStyledTexts(pair.getKey(), pair.getValue());
	}

	protected void printAutoStyledLine(String line, String additionalStyle) {
		Pair<String[], String[]> pair = createStyledTexts(line);
		printStyledTexts(pair.getKey(), pair.getValue(), additionalStyle);
	}

	protected void printStyledTexts(String[] texts, String[] styles) {
		String textArray = "["
				+ Stream.of(texts)
						.map((String text) -> "\"" + text.replaceAll("\"", "\\\"") + "\"")
						.collect(Collectors.joining(","))
				+ "]";
		String styleArray = "["
				+ Stream.of(styles)
						.map((String style) -> "\"" + style.replaceAll("\"", "\\\"") + "\"")
						.collect(Collectors.joining(","))
				+ "]";
		String script = String.format("printStyledTexts(%s, %s);", textArray, styleArray);
		webEngine.executeScript(script);
	}

	protected void printStyledTexts(String[] texts, String[] styles, String addtionalStyle) {
		for (int i = 0;  i < styles.length;  ++i) {
			styles[i] += addtionalStyle;
		}
		printStyledTexts(texts, styles);
	}

	protected Pair<String[], String[]> createStyledTexts(String s) {
		List<String> textList = new LinkedList<>();
		List<String> styleList = new LinkedList<>();
		s.codePoints().forEach((int codePoint) -> {
			textList.add(new String(new int[] {codePoint}, 0, 1));
			for (int i = 0, iMax = blockInfoList.size();  i < iMax;  ++i) {
				BlockInfo blockInfo = blockInfoList.get(i);
				if (codePoint >= blockInfo.first && codePoint <= blockInfo.last) {
					switch (blockInfo.name) {
					case "CJK Radicals Supplement":
					case "Kangxi Radicals":
					case "Ideographic Description Characters":
					case "CJK Strokes":
					case "CJK Compatibility":
					case "CJK Unified Ideographs Extension A":
					case "CJK Unified Ideographs":
					case "CJK Compatibility Ideographs":
					case "CJK Unified Ideographs Extension B":
					case "CJK Unified Ideographs Extension C":
					case "CJK Unified Ideographs Extension D":
					case "CJK Unified Ideographs Extension E":
					case "CJK Unified Ideographs Extension F":
					case "CJK Compatibility Ideohgraphs Supplement":
						styleList.add("font-family:'" + CheckBaseStage.this.fontToggleGroup.getSelectedToggle().getUserData() + "',HanaMinA,HanaMinB;");
						break;
					}
				}
			}
			if (textList.size() != styleList.size()) {
				styleList.add("font-family:monospace;");
			}
		});
		for (int i = 1;  i < styleList.size(); ) {
			if (styleList.get(i - 1).equals(styleList.get(i))) {
				textList.set(i - 1, textList.get(i - 1) + textList.get(i));
				styleList.remove(i);
				textList.remove(i);
			} else {
				++i;
			}
		}
		return new Pair<String[], String[]>(textList.toArray(new String[textList.size()]), styleList.toArray(new String[styleList.size()]));
	}

	protected void scrollToBottom() {
		webEngine.executeScript("window.scrollTo(0, document.body.scrollHeight);");
	}

	protected void findText(String text) {
		text = text.replaceAll("\"", "\\\"");
		webEngine.executeScript(String.format("window.find(/*aString:*/\"%s\", /*aCaseSensitive:*/true, /*aBackward:*/false, /*aWrapAround:*/true, /*aWholeWord:*/true, /*aSearchInFrame:*/true);", text));
	}

	////////////////////////

	public static void serializeDocument(Document document, OutputStream os) throws ClassNotFoundException, InstantiationException, IllegalAccessException, ClassCastException, TransformerFactoryConfigurationError, TransformerException {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(os);
		transformer.transform(source, result);
	}
}

////////////////////////////////
// BlockInfo
////////////////////////////////

class BlockInfo {

	public int first;
	public int last;
	public String name;

	public static List<BlockInfo> getBlockInfoList() {
		Pattern pattern = Pattern.compile("^([0-9A-F]+)\\.\\.([0-9A-F]+); (.+)$");
		try (InputStream is = IDSChecker.class.getClassLoader().getResourceAsStream("unicode/Blocks.txt");
				InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
				BufferedReader br = new BufferedReader(isr)) {
			List<BlockInfo> blockInfoList = br.lines()
					.map((String line) -> line.trim())
					.map((String line) -> pattern.matcher(line))
					.filter((Matcher matcher) -> matcher.matches())
					.map((Matcher matcher) -> {
						BlockInfo blockInfo = new BlockInfo();
						blockInfo.first = Integer.parseInt(matcher.group(1), 16);
						blockInfo.last = Integer.parseInt(matcher.group(2), 16);
						blockInfo.name = matcher.group(3);
						return blockInfo;
					})
					.collect(Collectors.toUnmodifiableList());
			return blockInfoList;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static BlockInfo getBlockInfo(int codePoint) {
		for (BlockInfo blockInfo : getBlockInfoList()) {
			if (codePoint >= blockInfo.first && codePoint <= blockInfo.last) {
				return blockInfo;
			}
		}
		throw new RuntimeException(String.format("%c is in not any block", codePoint));
	}
}

////////////////////////////////
// Check1
////////////////////////////////

class Check1Stage extends CheckBaseStage {

	public Check1Stage(File IDS_DIR) throws IOException {
		super(IDS_DIR);
		this.setTitle("Check1");
	}

	@Override
	public void check() {
		Platform.runLater(() -> {
			super.clearMain();
			super.printStyledLine("Check1", "font-size:200%;");
			super.printLine("pattern: ^(\\\\S+)\\\\s+(\\\\S+)(?:\\\\t(\\\\S+))*");
			super.printStyledLine("<HT>:\u21d2, <SPC>:\u21d4", "color:red;");
			super.printLine("================");
			super.scrollToBottom();
		});
		File[] files = IDS_DIR.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("IDS-") && name.endsWith(".txt");
			}
		});
		Pattern pattern = Pattern.compile("^(\\S+)\\s+(\\S+)(?:\\t(\\S+))*$");
		for (File file : files) {
			Platform.runLater(() -> {
				super.printStyledLine(file.getName(), "font-size:120%; font-weight:bold;");
				super.scrollToBottom();
			});
			try (FileInputStream fis = new FileInputStream(file);
					InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
					BufferedReader br = new BufferedReader(isr)) {
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (line.startsWith(";;") || line.startsWith("<;;") || line.isEmpty()) {
						continue;
					}
					Matcher matcher = pattern.matcher(line);
					if (!matcher.matches()) {
						final String fLine = line;
						Platform.runLater(() -> {
							super.printAutoStyledLine(fLine.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
							super.printStyledLine("unmatched with the pattern", "color:red;");
							super.scrollToBottom();
						});
						continue;
					}
				}
			} catch (IOException e) {
				Platform.runLater(() -> {
					super.printStyledLine(e.getMessage(), "color:red;");
					super.scrollToBottom();
				});
			}
		}
		Platform.runLater(() -> {
			super.printLine("================");
			super.printLine("done.");
			super.scrollToBottom();
		});
	}
}

////////////////////////////////
// Check2
////////////////////////////////

class Check2Stage extends CheckBaseStage {

	public Check2Stage(File IDS_DIR) throws IOException {
		super(IDS_DIR);
		this.setTitle("Check2");
	}

	@Override
	public void check() {
		Platform.runLater(() -> {
			super.clearMain();
			super.printStyledLine("Check2", "font-size:200%;");
			super.printLine("target: IDS-UCS-*.txt (without *Compat*)");
			super.printLine("pattern: ^U[-+]([0-9A-F]+)\\\\t(\\\\S+)(?:\\\\t(\\\\S+))*$");
			super.printStyledLine("<HT>:\u21d2, <SPC>:\u21d4", "color:red;");
			super.printLine("================");
			super.scrollToBottom();
		});
		File[] files = IDS_DIR.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("IDS-UCS") && name.endsWith(".txt") && name.indexOf("Compat") < 0;
			}
		});
		Pattern pattern = Pattern.compile("^U[-+]([0-9A-F]+)\\t(\\S+)(?:\\t(\\S+))*$");
		for (final File file : files) {
			Platform.runLater(() -> {
				super.printStyledLine(file.getName(), "font-size:120%; font-weight:bold;");
				super.scrollToBottom();
			});
			try (FileInputStream fis = new FileInputStream(file);
					InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
					BufferedReader br = new BufferedReader(isr)) {
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (line.startsWith(";;") || line.startsWith("<;;") || line.isEmpty()) {
						continue;
					}
					Matcher matcher = pattern.matcher(line);
					if (!matcher.matches()) {
						final String fLine = line;
						Platform.runLater(() -> {
							super.printAutoStyledLine(fLine.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
							super.printStyledLine("unmatched with the pattern", "color:red;");
							super.scrollToBottom();
						});
						continue;
					}
				}
			} catch (IOException e) {
				Platform.runLater(() -> {
					super.printStyledLine(e.getMessage(), "color:red;");
					super.scrollToBottom();
				});
			}
		}
		Platform.runLater(() -> {
			super.printLine("================");
			super.printLine("done.");
			super.scrollToBottom();
		});
	}
}

////////////////////////////////
// Check3
////////////////////////////////

class Check3Stage extends CheckBaseStage {

	public Check3Stage(File IDS_DIR) throws IOException {
		super(IDS_DIR);
		this.setTitle("Check3");
	}

	@Override
	public void check() {
		Platform.runLater(() -> {
			super.printStyledLine("Check3", "font-size:200%;");
			super.printLine("target: IDS-UCS-*.txt (without *Compat*)");
			super.printLine("pattern: ^<IDENTIFIER>\\\\t<CHARACTER>(?:\\\\t<DESCRIPTION>)*$");
			super.printStyledLine("<HT>:\u21d2, <SPC>:\u21d4", "color:red;");
			super.printLine("================");
			super.scrollToBottom();
		});
		File[] files = IDS_DIR.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("IDS-UCS") && name.endsWith(".txt") && name.indexOf("Compat") < 0;
			}
		});
		for (File file : files) {
			Platform.runLater(() -> {
				super.printStyledLine(file.getName(), "font-size:120%; font-weight:bold;");
				super.scrollToBottom();
			});
			Pattern pattern = Pattern.compile("^U[-+]([0-9A-F]+)\\t(\\S+)(?:\\t(\\S+))*$");
			try (FileInputStream fis = new FileInputStream(file);
					InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
					BufferedReader br = new BufferedReader(isr)) {
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (line.startsWith(";;") || line.startsWith("<;;") || line.isEmpty()) {
						continue;
					}
					Matcher matcher = pattern.matcher(line);
					if (!matcher.matches()) {
						final String fLine = line;
						Platform.runLater(() -> {
							super.printAutoStyledLine(fLine.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
							super.printStyledLine("unmatched with the pattern", "color:red;");
							super.scrollToBottom();
						});
						continue;
					}
					int idCodePoint = Integer.parseInt(matcher.group(1), 16);
					String character = matcher.group(2);
					try {
						Pair<IDBase, String> pair = IDBase.parse(character);
						if (!(pair.getKey() instanceof IDBase.NormalCharacter)) {
							final String fLine = line.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4");
							Platform.runLater(() -> {
								super.printAutoStyledLine(fLine);
								super.printStyledLine("character part is not a character", "color:red;");
								super.scrollToBottom();
							});
						}
						if (!pair.getValue().isEmpty()) {
							final String fLine = line;
							Platform.runLater(() -> {
								super.printAutoStyledLine(fLine.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
								super.printStyledLine("character part has unnecessary characters", "color:red;");
								super.scrollToBottom();
							});
						}
					} catch (IDBase.ParseException e) {
						final String fLine = line;
						Platform.runLater(() -> {
							super.printAutoStyledLine(fLine.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
							super.printStyledLine(e.getMessage(), "color:red;");
							super.scrollToBottom();
						});
					}
					for (int i = 3, iMax = matcher.groupCount();  i <= iMax;  ++i) {
						String description = matcher.group(i);
						// 規格としては groupCount() + 1 個 group が あるはずだが、、、
						if (description == null || description.isEmpty()) {
							continue;
						}
						try {
							Pair<IDBase, String> pair = IDBase.parse(description);
							// 偶に [X] や [J] が 附くことが ある
							if (!pair.getValue().isEmpty() && !pair.getValue().matches("\\[[A-Z]\\]")) {
								final String fLine = line;
								final String unnecessary = pair.getValue();
								Platform.runLater(() -> {
									super.printAutoStyledLine(fLine.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
									super.printAutoStyledLine("description part has unnecessary characters: " + unnecessary, "color:red;");
									super.scrollToBottom();
								});
							}
						} catch (IDBase.ParseException e) {
							final String fLine = line;
							Platform.runLater(() -> {
								super.printAutoStyledLine(fLine.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
								super.printStyledLine(e.getMessage(), "color:red;");
								super.scrollToBottom();
							});
						}
					}
				}
			} catch (IOException e) {
				Platform.runLater(() -> {
					super.printStyledLine(e.getMessage(), "color:red;");
					super.scrollToBottom();
				});
			}
		}
		Platform.runLater(() -> {
			super.printLine("================");
			super.printLine("done.");
			super.scrollToBottom();
		});
	}
}

////////////////////////////////
//Check4
////////////////////////////////

class Check4Stage extends CheckBaseStage {

	public Check4Stage(File IDS_DIR) throws IOException {
		super(IDS_DIR);
		this.setTitle("Check4");
	}

	private Map<String, Pair<String, String>> entityDictionary = new HashMap<>();

	@Override
	public void check() {
		Platform.runLater(() -> {
			super.printStyledLine("Check4", "font-size:200%;");
			super.printLine("target: IDS-UCS-*.txt (without *Compat*)");
			super.printLine("pattern: ^<IDENTIFIER>\\\\t<CHARACTER>(?:\\\\t<DESCRIPTION>)*$");
			super.printStyledLine("<HT>:\u21d2, <SPC>:\u21d4", "color:red;");
			super.printLine("================");
			super.scrollToBottom();
		});
		File[] files = IDS_DIR.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("IDS-UCS") && name.endsWith(".txt") && name.indexOf("Compat") < 0;
			}
		});
		for (File file : files) {
			Platform.runLater(() -> {
				super.printStyledLine(file.getName(), "font-size:120%; font-weight:bold;");
				super.scrollToBottom();
			});
			Pattern pattern = Pattern.compile("^U[-+]([0-9A-F]+)\\t(\\S+)(?:\\t(\\S+))*$");
			try (FileInputStream fis = new FileInputStream(file);
					InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
					BufferedReader br = new BufferedReader(isr)) {
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (line.startsWith(";;") || line.startsWith("<;;") || line.isEmpty()) {
						continue;
					}
					Matcher matcher = pattern.matcher(line);
					if (!matcher.matches()) {
						final String fLine = line;
						Platform.runLater(() -> {
							super.printAutoStyledLine(fLine.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
							super.printStyledLine("unmatched with the pattern", "color:red;");
							super.scrollToBottom();
						});
						continue;
					}
					////////////////
					// identifier
					////////////////
					int idCodePoint = Integer.parseInt(matcher.group(1), 16);
System.out.println(String.format("U+%04X", idCodePoint));
					////////////////
					// character
					////////////////
					String character = matcher.group(2);
					try {
						Pair<IDBase, String> pair = IDBase.parse(character);
						if (!(pair.getKey() instanceof IDBase.NormalCharacter)) {
							final String fLine = line;
							Platform.runLater(() -> {
								super.printAutoStyledLine(fLine.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
								super.printStyledLine("character part is not a character", "color:red;");
								super.scrollToBottom();
							});
						}
						if (!pair.getValue().isEmpty()) {
							final String fLine = line;
							Platform.runLater(() -> {
								super.printAutoStyledLine(fLine.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
								super.printStyledLine("character part has unnecessary characters", "color:red;");
								super.scrollToBottom();
							});
						}
					} catch (IDBase.ParseException e) {
						final String fLine = line;
						Platform.runLater(() -> {
							super.printAutoStyledLine(fLine.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
							super.printStyledLine(e.getMessage(), "color:red;");
							super.scrollToBottom();
						});
					}
					////////////////
					// descriptions
					////////////////
					for (int i = 3, iMax = matcher.groupCount();  i <= iMax;  ++i) {
						String description = matcher.group(i);
						if (description == null || description.isEmpty()) {
							continue;
						}
						try {
							Pair<IDBase, String> pair = IDBase.parse(description);
							String rest = pair.getValue();
							if (rest != null && !rest.isEmpty()) {
								final String fLine = line;
								Platform.runLater(() -> {
									super.printAutoStyledLine(fLine.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
									super.printStyledLine("unnecessary character: " + rest, "color:red;");
									super.scrollToBottom();
								});
								continue;
							}
							checkDescription(line, pair.getKey(), /*depth:*/0);
						} catch (IDBase.ParseException e) {
							final String fLine = line;
							Platform.runLater(() -> {
								super.printAutoStyledLine(fLine.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
								super.printStyledLine(e.getMessage(), "color:red;");
								super.scrollToBottom();
							});
						}
					}
				}
			} catch (IOException e) {
				Platform.runLater(() -> {
					super.printStyledLine(e.getMessage(), "color:red;");
					super.scrollToBottom();
				});
			}
		}
		Platform.runLater(() -> {
			super.printLine("================");
			super.printLine("done.");
			super.scrollToBottom();
		});
	}

	private void checkDescription(String line, IDBase idBase, int depth) {
		if (idBase instanceof IDBase.NormalCharacter) {
			int codePoint = ((IDBase.NormalCharacter) idBase).codePoint;
			switch (BlockInfo.getBlockInfo(codePoint).name) {
			case "CJK Radicals Supplement":
			case "Kangxi Radicals":
			//case "Ideographic Description Characters":
			case "CJK Strokes":
			//case "CJK Compatibility":
			case "CJK Unified Ideographs Extension A":
			case "CJK Unified Ideographs":
			//case "CJK Compatibility Ideographs":
			case "CJK Unified Ideographs Extension B":
			case "CJK Unified Ideographs Extension C":
			case "CJK Unified Ideographs Extension D":
			case "CJK Unified Ideographs Extension E":
			case "CJK Unified Ideographs Extension F":
			//case "CJK Compatibility Ideohgraphs Supplement":
				return;
			}
			Platform.runLater(() -> {
				super.printAutoStyledLine(line.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
				super.printStyledLine(String.format("undesirable character '%c' (U+%04X) (%s)", codePoint, codePoint, BlockInfo.getBlockInfo(codePoint).name), "color:red;");
				super.scrollToBottom();
			});
			return;
		} else if (idBase instanceof IDBase.IDCharacter) {
//			int idcCodePoint = ((IDBase.IDCharacter) idBase).idcCodePoint;
			List<IDBase> parameterList = ((IDBase.IDCharacter) idBase).parameterList;
			for (IDBase parameter : parameterList) {
				checkDescription(line, parameter, depth + 1);
			}
		} else if (idBase instanceof IDBase.EntityCharacter) {
			String entityName = ((IDBase.EntityCharacter) idBase).entityName;
			if (entityDictionary.containsKey(entityName)) {
				if (entityDictionary.get(entityName).getKey() != null) {
					// TODO
				} else {
					// TODO
				}
				return;
			}
			File[] files = IDS_DIR.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith("IDS-") && name.endsWith(".txt");
				}
			});
			Pattern pattern = Pattern.compile("^(\\S+)\\s+(\\S+)(?:\\s+(\\S+))*$");
			boolean found = false;
			for (File file : files) {
				try {
					Optional<Pair<String, Matcher>> result = Files.lines(file.toPath())
							.map((String line2) -> line2.trim())
							.filter((String line2) -> !line2.startsWith(";;") && !line2.startsWith("<;;"))
							.map((String line2) -> new Pair<String, Matcher>(line2, pattern.matcher(line2)))
							.filter((Pair<String, Matcher> pair2) -> pair2.getValue().matches())
							.filter((Pair<String, Matcher> pair2) -> {
								if (pair2.getValue().group(1).equals(entityName)) {
									Platform.runLater(() -> {
										super.printAutoStyledLine(line.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
										super.printStyledLine(entityName + " found in " + file.getName(), "color:blue");
										super.printAutoStyledLine(pair2.getKey().replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"), "color:blue;");
										super.scrollToBottom();
									});
									entityDictionary.put(entityName, new Pair<String, String>(pair2.getKey(), null));
									return true;
								} else {
									return false;
								}
							})
							.findFirst();
					if (result.isPresent()) {
						found = true;
						break;
					} else {
						continue;
					}
				} catch (IOException e) {
					// TODO
				}
			}
			if (!found) {
				Platform.runLater(() -> {
					super.printAutoStyledLine(line.replaceAll("\\t", "\u21d2").replaceAll(" ", "\u21d4"));
					super.printStyledLine(entityName + " is not found", "color:red");
					super.scrollToBottom();
				});
				entityDictionary.put(entityName, new Pair<>(null, null));
			}
		}
	}
}

////////////////////////////////
// description with "Ideographic Description Characters"
////////////////////////////////

abstract class IDBase {

	public abstract String toString();

	public static Pair<IDBase, String> parse(String s) throws ParseException {
		if (s == null || s.isEmpty()) {
			throw new ParseException("necessary character doesn't exist");
		}
		char c = s.charAt(0);
		if (c == '&') {
			int index = s.indexOf(';');
			if (index < 0) {
				throw new ParseException("expected ';'");
			}
			EntityCharacter object = new EntityCharacter();
			object.entityName = s.substring(1, index);
			String rest = s.substring(index + 1);
			return new Pair<>(object, rest);
		}
		if (IDCharacter.idc2s.indexOf(c) >= 0) {
			IDCharacter object = new IDCharacter();
			object.idcCodePoint = s.codePointAt(0);
			s = s.substring(1);
			Pair<IDBase, String> pair = parse(s);
			object.parameterList.add(pair.getKey());
			pair = parse(pair.getValue());
			object.parameterList.add(pair.getKey());
			String rest = pair.getValue();
			return new Pair<>(object, rest);
		}
		if (IDCharacter.idc3s.indexOf(c) >= 0) {
			IDCharacter object = new IDCharacter();
			object.idcCodePoint = s.codePointAt(0);
			s = s.substring(1);
			Pair<IDBase, String> pair = parse(s);
			object.parameterList.add(pair.getKey());
			pair = parse(pair.getValue());
			object.parameterList.add(pair.getKey());
			pair = parse(pair.getValue());
			object.parameterList.add(pair.getKey());
			String rest = pair.getValue();
			return new Pair<>(object, rest);
		}
		NormalCharacter object = new NormalCharacter();
		object.codePoint = s.codePointAt(0);
		if (object.codePoint >= 0x10000) {
			return new Pair<>(object, s.substring(2));
		} else {
			return new Pair<>(object, s.substring(1));
		}
	}

	public static class ParseException extends Exception {
		private static final long serialVersionUID = 1L;
		public ParseException(String message) {
			super(message);
		}
	}

	public static class IDCharacter extends IDBase {
		public static String idcs = "⿰⿱⿲⿳⿴⿵⿶⿷⿸⿹⿺⿻";
		public static String idc2s = "⿰⿱⿴⿵⿶⿷⿸⿹⿺⿻";
		public static String idc3s = "⿲⿳";

		protected int idcCodePoint;
		protected List<IDBase> parameterList = new LinkedList<>();

		@Override
		public String toString() {
			return new String(new int[] {idcCodePoint}, 0, 1)
					+ parameterList.stream()
					.map((IDBase idBase) -> idBase.toString())
					.collect(Collectors.joining());
		}
	}

	public static class NormalCharacter extends IDBase {
		protected int codePoint;

		@Override
		public String toString() {
			return new String(new int[] {codePoint}, 0, 1);
		}
	}

	public static class EntityCharacter extends IDBase {
		protected String entityName;

		@Override
		public String toString() {
			return "&" + entityName + ";";
		}
	}
}
