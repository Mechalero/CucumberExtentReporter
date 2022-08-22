package com.mechalero.cucumber.listener;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.GherkinKeyword;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.aventstack.extentreports.reporter.ExtentXReporter;
import com.aventstack.extentreports.reporter.KlovReporter;
import com.mongodb.MongoClientURI;

import io.cucumber.core.internal.gherkin.ast.Examples;
import io.cucumber.core.internal.gherkin.ast.Feature;
import io.cucumber.core.internal.gherkin.ast.Node;
import io.cucumber.core.internal.gherkin.ast.Scenario;
import io.cucumber.core.internal.gherkin.ast.ScenarioDefinition;
import io.cucumber.core.internal.gherkin.ast.ScenarioOutline;
import io.cucumber.core.internal.gherkin.ast.Step;
import io.cucumber.core.internal.gherkin.ast.TableCell;
import io.cucumber.core.internal.gherkin.ast.TableRow;
import io.cucumber.core.internal.gherkin.ast.Tag;
import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.DataTableArgument;
import io.cucumber.plugin.event.DocStringArgument;
import io.cucumber.plugin.event.EmbedEvent;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.StepArgument;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestSourceRead;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.TestStepStarted;
import io.cucumber.plugin.event.WriteEvent;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExtentCucumberFormatter implements EventListener {

	private final TestSourcesModel testSources;
	private static ExtentReports extentReports;
	private static ExtentHtmlReporter htmlReporter;
	private static KlovReporter klovReporter;

	private URI currentFeatureFile;
	private ScenarioOutline currentScenarioOutline;
	private Examples currentExamples;
	private static ThreadLocal<ExtentTest> featureTestThreadLocal = new InheritableThreadLocal<>();
	private static ThreadLocal<ExtentTest> scenarioOutlineThreadLocal = new InheritableThreadLocal<>();
	private static Map<String, ExtentTest> scenarioOutlineMap = new ConcurrentHashMap<>();
	static ThreadLocal<ExtentTest> scenarioThreadLocal = new InheritableThreadLocal<>();
	private static ThreadLocal<LinkedList<Step>> stepListThreadLocal = new InheritableThreadLocal<>();
	static ThreadLocal<ExtentTest> stepTestThreadLocal = new InheritableThreadLocal<>();
	private boolean scenarioOutlineFlag;
//	private int embeddedIndex;

	private static final Map<String, String> MIME_TYPES_EXTENSIONS = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;

		{
			put("image/bmp", "bmp");
			put("image/gif", "gif");
			put("image/jpeg", "jpg");
			put("image/png", "png");
			put("image/svg+xml", "svg");
			put("video/ogg", "ogg");
		}
	};

	public ExtentCucumberFormatter(File file) {
		testSources = new TestSourcesModel();
		setExtentHtmlReport(file);
		setExtentReport();
		setKlovReport();
		stepListThreadLocal.set(new LinkedList<>());
		scenarioOutlineFlag = false;
	}

	@Override
	public void setEventPublisher(EventPublisher publisher) {
		publisher.registerHandlerFor(TestSourceRead.class, this::handleTestSourceRead);
		publisher.registerHandlerFor(TestCaseStarted.class, this::handleTestCaseStarted);
		publisher.registerHandlerFor(TestStepStarted.class, this::handleTestStepStarted);
		publisher.registerHandlerFor(TestStepFinished.class, this::handleTestStepFinished);
		publisher.registerHandlerFor(EmbedEvent.class, this::handleEmbed);
		publisher.registerHandlerFor(WriteEvent.class, this::handleWrite);
		publisher.registerHandlerFor(TestRunFinished.class, event -> finishReport());

	}

	private void handleTestSourceRead(TestSourceRead event) {
		testSources.addTestSourceReadEvent(event.getUri(), event);
	}

	private void handleTestCaseStarted(TestCaseStarted event) {
		handleStartOfFeature(event.getTestCase());
		handleScenarioOutline(event.getTestCase());
		createTestCase(event.getTestCase());
		if (testSources.hasBackground(currentFeatureFile, event.getTestCase().getLine())) {
			// createBackground(event.getTestCase());
		}
	}

	private void handleTestStepStarted(TestStepStarted event) {
		if (event.getTestStep() instanceof PickleStepTestStep) {
			PickleStepTestStep testStep = (PickleStepTestStep) event.getTestStep();
//            if (isFirstStepAfterBackground(testStep)) {
//                jsFunctionCall("scenario", currentTestCaseMap);
//                currentTestCaseMap = null;
//            }
			createTestStep(testStep);
			createMatchMap((PickleStepTestStep) event.getTestStep());
		}
	}

	private void handleTestStepFinished(TestStepFinished event) {
		updateResult(event.getResult());
	}

	private void handleEmbed(EmbedEvent event) {
		String mediaType = event.getMediaType();
		if (mediaType.startsWith("text/")) {
//            jsFunctionCall("embedding", mediaType, new String(event.getData()), event.getName());
		} else {

			String extension = MIME_TYPES_EXTENSIONS.get(mediaType);
			if (extension != null) {
//                StringBuilder fileName = new StringBuilder("embedded").append(embeddedIndex++).append(".").append(extension);
//                writeBytesToURL(event.getData(), toUrl(fileName.toString()));
			}
		}
	}

	private void handleWrite(WriteEvent event) {
		String text = event.getText();
		if (text != null && !text.isEmpty()) {
			stepTestThreadLocal.get().info(text);
		}
	}

	private void finishReport() {
		getExtentReport().flush();
	}

	private void handleStartOfFeature(TestCase testCase) {
		if (currentFeatureFile == null || !currentFeatureFile.equals(testCase.getUri())) {
			currentFeatureFile = testCase.getUri();
			createFeature(testCase);
		}
	}

	public void createFeature(TestCase testCase) {
		Feature feature = testSources.getFeature(testCase.getUri());
		if (feature != null) {
			featureTestThreadLocal.set(getExtentReport()
					.createTest(com.aventstack.extentreports.gherkin.model.Feature.class, feature.getName()));
			if (!feature.getTags().isEmpty())
				createTagList(feature.getTags());
		}
	}

	private void createTagList(List<Tag> tags) {
		tags.stream().map(Tag::getName).forEach(featureTestThreadLocal.get()::assignCategory);
	}

	private void handleScenarioOutline(TestCase testCase) {
		TestSourcesModel.AstNode astNode = testSources.getAstNode(currentFeatureFile, testCase.getLine());
		if (TestSourcesModel.isScenarioOutlineScenario(astNode)) {
			ScenarioOutline scenarioOutline = (ScenarioOutline) TestSourcesModel.getScenarioDefinition(astNode);
			if (currentScenarioOutline == null || !currentScenarioOutline.equals(scenarioOutline)) {
				currentScenarioOutline = scenarioOutline;
				createScenarioOutline(currentScenarioOutline);
				addOutlineStepsToReport(scenarioOutline);
			}
			Examples examples = (Examples) astNode.parent.node;
			if (currentExamples == null || !currentExamples.equals(examples)) {
				currentExamples = examples;
				createExamples(examples);
			}
		} else {
			scenarioOutlineThreadLocal.set(null);
			currentScenarioOutline = null;
			currentExamples = null;
		}
	}

	private synchronized void addOutlineStepsToReport(ScenarioOutline scenarioOutline) {
		for (Step step : scenarioOutline.getSteps()) {
			if (step.getArgument() != null) {
				Node argument = step.getArgument();
				if (argument instanceof DocStringArgument) {
					createDocStringMap((DocStringArgument) argument);
				} else if (argument instanceof DataTableArgument) {

				}
			}
		}
	}

	public void createScenarioOutline(ScenarioOutline scenarioOutline) {
		if (scenarioOutlineMap.containsKey(scenarioOutline.getName())) {
			scenarioOutlineThreadLocal.set(scenarioOutlineMap.get(scenarioOutline.getName()));
			return;
		}

		if (scenarioOutlineThreadLocal.get() == null) {
			ExtentTest t = featureTestThreadLocal.get()
                    .createNode(com.aventstack.extentreports.gherkin.model.ScenarioOutline.class,
                            scenarioOutline.getName(), scenarioOutline.getDescription());
            scenarioOutlineThreadLocal.set(t);
            scenarioOutlineMap.put(scenarioOutline.getName(), t);
		}
	}

	public void createExamples(Examples examples) {
		ExtentTest test = scenarioOutlineThreadLocal.get();

		String[][] data = null;
		List<TableRow> rows = examples.getTableBody();
		int rowSize = rows.size();
		for (int i = 0; i < rowSize; i++) {
			TableRow examplesTableRow = rows.get(i);
			List<TableCell> cells = examplesTableRow.getCells();
			int cellSize = cells.size();
			if (data == null) {
				data = new String[rowSize][cellSize];
			}
			for (int j = 0; j < cellSize; j++) {
				data[i][j] = cells.get(j).getValue();
			}
		}
		test.info(MarkupHelper.createTable(data));
	}

	private void createTestCase(TestCase testCase) {
		TestSourcesModel.AstNode astNode = testSources.getAstNode(currentFeatureFile, testCase.getLine());
		if (astNode != null) {
			ScenarioDefinition scenarioDefinition = TestSourcesModel.getScenarioDefinition(astNode);
			
			ExtentTest parent = scenarioOutlineThreadLocal.get() != null
                    ? scenarioOutlineThreadLocal.get()
                    : featureTestThreadLocal.get();
			
			ExtentTest t = parent.createNode(
					com.aventstack.extentreports.gherkin.model.Scenario.class, scenarioDefinition.getName(),
					scenarioDefinition.getDescription());
			scenarioThreadLocal.set(t);
		}
		if (!testCase.getTags().isEmpty())
			testCase.getTags().forEach(scenarioThreadLocal.get()::assignCategory);
	}

	public void createTestStep(PickleStepTestStep testStep) {
		String stepName = testStep.getStep().getText();
		StepArgument argument = testStep.getStep().getArgument();
		if (argument != null) {
			if (argument instanceof DocStringArgument) {
				DocStringArgument docStringArgument = (DocStringArgument) argument;
				createDocStringMap(docStringArgument);
			} else if (argument instanceof DataTableArgument) {
				DataTableArgument dataTableArgument = (DataTableArgument) argument;
				stepTestThreadLocal.get()
						.pass(MarkupHelper.createTable(createDataTableList(dataTableArgument.cells())).getMarkup());
			}
		}
		TestSourcesModel.AstNode astNode = testSources.getAstNode(currentFeatureFile, testStep.getStep().getLine());
		if (astNode != null) {
			Step step = (Step) astNode.node;
			try {
				ExtentTest t = scenarioThreadLocal.get().createNode(new GherkinKeyword(step.getKeyword().trim()),
						stepName);
				stepTestThreadLocal.set(t);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}

	}

	/**
	 * 
	 * @param docString
	 * 
	 * @apiNote Refactorizar: no está haciendo nada en el reporte.
	 */
	private void createDocStringMap(DocStringArgument docString) {
		Map<String, Object> docStringMap = new HashMap<>();
		docStringMap.put("value", docString.getContent());
	}

	private String[][] createDataTableList(List<List<String>> rows) {
		String data[][] = null;

		int rowSize = rows.size();
		for (int i = 0; i < rowSize; i++) {
			List<String> row = rows.get(i);
			int cellSize = row.size();
			if (data == null)
				data = new String[rowSize][cellSize];
			for (int j = 0; j < cellSize; j++)
				data[i][j] = row.get(j);
		}
		return data;
	}

	/**
	 * 
	 * @param testStep
	 * 
	 * @apiNote Refactorizar: no está haciendo nada al reporte
	 */
	private void createMatchMap(PickleStepTestStep testStep) {
		Map<String, Object> matchMap = new HashMap<>();
		String location = testStep.getCodeLocation();
		if (location != null) {
			matchMap.put("location", location);
		}
	}

	public void updateResult(Result result) {
		if (scenarioOutlineFlag) {
			return;
		}

		switch (result.getStatus()) {
		case FAILED:
			stepTestThreadLocal.get().fail(result.getError());
			break;
		case UNDEFINED:
//			if (strict) {
//				stepTestThreadLocal.get().fail(Status.UNDEFINED.name());
//				break;
//			}
			stepTestThreadLocal.get().skip(Status.UNDEFINED.name());
			break;
		case PENDING:
		case SKIPPED:

//			if (isHookThreadLocal.get()) {
//				ExtentService.getInstance().removeTest(stepTestThreadLocal.get());
//				break;
//			}
//			boolean currentEndingEventSkipped = test.hasLog()
//					? test.getLogs().get(test.getLogs().size() - 1).getStatus() == Status.SKIPPED
//					: false;()
			if (result.getError() != null) {
				stepTestThreadLocal.get().skip(result.getError().getMessage());
			} // else if (!currentEndingEventSkipped) {
//				String details = result.getErrorMessage() == null ? "Step skipped" : result.getErrorMessage();
//				stepTestThreadLocal.get().skip(details);
//			}
			break;
		case PASSED:
			stepTestThreadLocal.get().pass("");
//			if (stepTestThreadLocal.get() != null && !test.hasLog() && !isHookThreadLocal.get())
//				stepTestThreadLocal.get().pass("");
//			if (stepTestThreadLocal.get() != null) {
//				Boolean hasScreenCapture = test.hasLog() && test.getLogs().get(0).hasMedia();
//				if (isHookThreadLocal.get() && !test.hasLog() && !hasScreenCapture)
//					ExtentService.getInstance().removeTest(stepTestThreadLocal.get());
//			}
			break;
		default:
			break;
		}

	}

//	private URL toUrl(String fileName) {
//        try {
//            return new URL(, fileName);
//        } catch (IOException e) {
//            throw new CucumberException(e);
//        }
//    }

	private static void setExtentHtmlReport(File file) {
		if (htmlReporter != null) {
			return;
		}
		if (file == null || file.getPath().isEmpty()) {
			file = new File(ExtentProperties.INSTANCE.getReportPath());
		}
		if (!file.exists()) {
			file.getParentFile().mkdirs();
		}
		htmlReporter = new ExtentHtmlReporter(file);
	}

	static ExtentHtmlReporter getExtentHtmlReport() {
		return htmlReporter;
	}

	private static void setExtentReport() {
		if (extentReports != null) {
			return;
		}
		extentReports = new ExtentReports();
		ExtentProperties extentProperties = ExtentProperties.INSTANCE;

		// Remove this block in the next release
		if (extentProperties.getExtentXServerUrl() != null) {
			String extentXServerUrl = extentProperties.getExtentXServerUrl();
			try {
				URL url = new URL(extentXServerUrl);
				ExtentXReporter xReporter = new ExtentXReporter(url.getHost());
				xReporter.config().setServerUrl(extentXServerUrl);
				xReporter.config().setProjectName(extentProperties.getProjectName());
				extentReports.attachReporter(htmlReporter, xReporter);
				return;
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("Invalid ExtentX Server URL", e);
			}
		}
		extentReports.attachReporter(htmlReporter);
	}

	static ExtentReports getExtentReport() {
		return extentReports;
	}

	private static synchronized void setKlovReport() {
		if (extentReports == null) {
			// Extent reports object not found. call setExtentReport() first
			return;
		}

		ExtentProperties extentProperties = ExtentProperties.INSTANCE;

		// if reporter is not null that means it is already attached
		if (klovReporter != null) {
			// Already attached, attaching it again will create a new build/klov report
			return;
		}

		if (extentProperties.getKlovServerUrl() != null) {
			String hostname = extentProperties.getMongodbHost();
			int port = extentProperties.getMongodbPort();

			String database = extentProperties.getMongodbDatabase();

			String username = extentProperties.getMongodbUsername();
			String password = extentProperties.getMongodbPassword();

			try {
				// Create a new KlovReporter object
				klovReporter = new KlovReporter();

				if (username != null && password != null) {
					MongoClientURI uri = new MongoClientURI("mongodb://" + username + ":" + password + "@" + hostname
							+ ":" + port + "/?authSource=" + database);
					klovReporter.initMongoDbConnection(uri);
				} else {
					klovReporter.initMongoDbConnection(hostname, port);
				}

				klovReporter.setProjectName(extentProperties.getKlovProjectName());
				klovReporter.setReportName(extentProperties.getKlovReportName());
				klovReporter.setKlovUrl(extentProperties.getKlovServerUrl());

				extentReports.attachReporter(klovReporter);

			} catch (Exception ex) {
				klovReporter = null;
				throw new IllegalArgumentException("Error setting up Klov Reporter", ex);
			}
		}
	}

	static KlovReporter getKlovReport() {
		return klovReporter;
	}

	public void startOfScenarioLifeCycle(Scenario scenario) {
		if (scenarioOutlineFlag) {
			scenarioOutlineFlag = false;
		}

		ExtentTest scenarioNode;
		if (scenarioOutlineThreadLocal.get() != null
				&& scenario.getKeyword().trim().equalsIgnoreCase("Scenario Outline")) {
			scenarioNode = scenarioOutlineThreadLocal.get()
					.createNode(com.aventstack.extentreports.gherkin.model.Scenario.class, scenario.getName());
		} else {
			scenarioNode = featureTestThreadLocal.get()
					.createNode(com.aventstack.extentreports.gherkin.model.Scenario.class, scenario.getName());
		}

		for (Tag tag : scenario.getTags()) {
			scenarioNode.assignCategory(tag.getName());
		}
		scenarioThreadLocal.set(scenarioNode);
	}

}
