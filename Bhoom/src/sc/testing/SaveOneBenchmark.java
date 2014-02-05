package sc.testing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import sc.engine.SearchEngine;
import sc.engine.engines.AbstractEngine.SearchMode;
import sc.engine.engines.CTestEngine;
import sc.engine.movesorter.MvvLvaHashSorter;
import sc.engine.ttables.AlwaysReplace;
import sc.evaluators.SideToMoveEvaluator;
import sc.testing.TestingUtils.EngineSetting;
import sc.testing.TestingUtils.SuiteResult;
import sc.util.ObjectPool.Factory;

public class SaveOneBenchmark {

	public static File dir = new File("testing");
	public static File benchmark = new File(dir, "benchmarks");
	public static File suites = new File(dir, "suites");

	static private void saveBenchmark(String suiteFile,
			Factory<SearchEngine> engineFactory, EngineSetting setting,
			String saveFile) throws Exception {
		SuiteResult sr = TestingUtils.getTestResults(suiteFile, engineFactory,
				setting);
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				saveFile));
		oos.writeObject(sr);
		oos.close();
	}
	
	public static void main(String[] args) throws Exception {

		final EngineSetting setting = new EngineSetting();
		setting.depth = 7;
		setting.timeMs = 0;
		final File suiteFile = new File(suites, "Test20.EPD");
		Factory<SearchEngine> factory = new Factory<SearchEngine>() {

			@Override
			public SearchEngine create() {
				CTestEngine se = new CTestEngine("HashMoveSorter", SearchMode.ASP_WIN, new SideToMoveEvaluator(), new AlwaysReplace(), new MvvLvaHashSorter());
				se.setFlags(true, true, true, false, true, true, true);
				return se;
			}

			@Override
			public SearchEngine[] getArray(int size) {
				// TODO Auto-generated method stub
				return null;
			}
			
		};
		
		saveBenchmark(suiteFile.getAbsolutePath(), factory, setting, getResultFileName(setting, "HashMoveSorting", suiteFile));

	}
	
	static private String getResultFileName(EngineSetting setting,
			String engineName, File suite) {
		String settingString = null;
		if (setting.depth >= 100) {
			settingString = "" + (setting.timeMs / 1000) + "sec.ser";
		} else {
			settingString = "depth" + setting.depth;
		}
		String fileName = suite.getName();
		int lastDot = fileName.lastIndexOf(".");
		return fileName.substring(0, lastDot) + "-" + engineName + "-"
				+ settingString;
	}

}
