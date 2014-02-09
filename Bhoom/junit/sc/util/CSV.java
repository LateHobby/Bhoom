package sc.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class CSV {
	private String delimiter =",";
	
	List<String[]> lines = new ArrayList<String[]>();
	
	
	
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
	
	public void parse(String file) throws IOException {
		parse(new File(file));
	}

	public void parse(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = br.readLine()) != null) {
			if (line.trim().startsWith("#")) {
				continue;
			}
			lines.add(line.split(delimiter));
		}
		br.close();
	}
	
	
	public <T> List<T> createList(Class<T> clz) throws InstantiationException, IllegalAccessException {
		Field[] fields = getPublicFields(clz);
		Field[] fieldsByHeaderPosition = getFieldsByHeaderPosition(fields);
		List l = new ArrayList();
		for (String[] line : lines) {
			l.add(populate(clz, fieldsByHeaderPosition, line));
		}
		return l;
	}

	private <T> T populate(Class<T> clz, Field[] fieldsByHeaderPosition, String[] line ) throws InstantiationException, IllegalAccessException {
		T obj = clz.newInstance();
		int i = 0;
		for (Field f : fieldsByHeaderPosition) {
			if (f != null) {
				String value = line[i];
				fillField(obj, f, clz, value);
			}
			i++;
		}
		return obj;
	}
	
	private <T> void fillField(T obj, Field f, Class<T> clz, String value) throws NumberFormatException, IllegalArgumentException, IllegalAccessException {
		Class<?> c = f.getType();
		if (c.isAssignableFrom(int.class)) {
			f.setInt(obj, Integer.parseInt(value));
		} else if (c.isAssignableFrom(double.class)) {
			f.setDouble(obj, Double.parseDouble(value));
		} else if (c.isAssignableFrom(String.class)) {
			f.set(obj, value);
		}
	}
	
	private Field[] getFieldsByHeaderPosition(Field[] fields) {
		String[] header = lines.get(0);
		Field[] fa = new Field[header.length];
		int i = 0;
		for (String name : header) {
			for (Field f : fields) {
				if (name.trim().equalsIgnoreCase(f.getName())) {
					fa[i] = f;
				}
			}
			i++;
		}
		return fa;
	}
	
	public void write(String fileName) throws IOException {
		write(new File(fileName));
	}

	public void write(File file) throws IOException {
		PrintStream bw = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
		write(bw);
		bw.close();
	}

	public void write(PrintStream bw) {
		int i = 0;
		for (String[] line : lines) {
			for (String value: line) {
				bw.print(value);
				if (i < line.length-1) {
					bw.print(delimiter);
				}
			}
			bw.println();
			i++;
		}
	}
	
	public <T> void fillFrom(List<T> list, String[] headers) throws IllegalArgumentException, IllegalAccessException {
		lines.clear();
		lines.add(headers);
		if (list.isEmpty()) {
			return;
		}
		Class<?> clz = list.get(0).getClass();
		Field[] fields = getPublicFields(clz);
		Field[] fieldsByHeaderPosition = getFieldsByHeaderPosition(fields);
		
		for (T obj : list) {
			String[] sa = new String[headers.length];
			int i = 0;
			for (Field f : fieldsByHeaderPosition) {
				if (f == null) {
					sa[i] = "";
				} else {
					sa[i] = f.get(obj).toString();
				}
				i++;
			}
			lines.add(sa);
		}
		
	}
	
	private <T> Field[] getPublicFields(Class<T> clz) {
		Field[] fields = clz.getFields();
		List<Field> fl = new ArrayList<Field>();
		for (Field f : fields) {
			if (Modifier.isPublic(f.getModifiers())) {
				fl.add(f);
			}
		}
		return  fl.toArray(new Field[0]);
	}
}
