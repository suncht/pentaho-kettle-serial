package com.suncht.di.trans.steps;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;

public class TikaUtil {
	public static String getBody(File file) {
		InputStream input = null;
		try {
			Parser parser = new AutoDetectParser();
			input = new FileInputStream(file);
			Metadata matadata = new Metadata();
			BodyContentHandler textHandler = new BodyContentHandler(1000 * 1000);
			ParseContext context = new ParseContext();
			parser.parse(input, textHandler, matadata, context);//执行解析过程
			String content = textHandler.toString();
			//System.out.println(content);
			return content;
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return "";
	}

}
