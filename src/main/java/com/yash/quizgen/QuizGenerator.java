package com.yash.quizgen;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.univocity.parsers.common.processor.RowListProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class QuizGenerator {

	private static final String DB_HOSTNAME = "mysql.host";
	private static final String DB_PORT = "mysql.port";
	private static final String DB_USERNAME = "mysql.username";
	private static final String DB_PASSOWRD = "mysql.password";
	private static final String DB_NAME = "mysql.db_name";
	private static final String NO_OF_QUES_PER_QUIZ = "no_of_ques_per_quiz";
	private static final String TOTAL_NO_OF_QUES = "total_no_of_ques";
	private static final String FILENAME = "filename";
	private static final String TAGS = "tags";
	private static final String DIFFICULTY = "difficulty";
	
	private static Config config;
	private static List<String> tagsList;
	private static List<String> difficultyList;
	private static Map<String, Integer> difficultyCount = new HashMap<String, Integer>();
	private static Map<String, Integer> tagCount = new HashMap<String, Integer>();
	private static boolean isQuizGenPossible = true;
	
	private static void createSchema() throws SQLException {
		Connection connection = getConnection();
		Statement stmt = connection.createStatement();
		stmt.executeUpdate("CREATE TABLE questions (qid INT AUTO_INCREMENT, question VARCHAR(45), difficulty VARCHAR(10), tag VARCHAR(5), is_assigned TINYINT(1) DEFAULT 0, PRIMARY KEY (qid))");
		stmt.executeUpdate("CREATE TABLE quizzes (qzid INT AUTO_INCREMENT, qz_name VARCHAR(45), ques_id INT, PRIMARY KEY (qzid), FOREIGN KEY(`ques_id`) REFERENCES questions(`qid`) ON DELETE CASCADE)");
		connection.close();
    }
	
	private static void dropTables() throws SQLException {
		Connection connection = getConnection();
		Statement stmt = connection.createStatement();
		stmt.executeUpdate("DROP TABLE quizzes");
		stmt.executeUpdate("DROP TABLE questions");
		connection.close();
	}
	
	private static void insertFileDataToDB() throws SQLException {
		Connection connection = getConnection();
		String insertQuery = "INSERT INTO questions (question, difficulty, tag) VALUES (?, ?, ?)";
		PreparedStatement preparedStmt = connection.prepareStatement(insertQuery);
		CsvParserSettings parserSettings = new CsvParserSettings();
		parserSettings.setLineSeparatorDetectionEnabled(true);
		RowListProcessor rowProcessor = new RowListProcessor();
		parserSettings.setProcessor(rowProcessor);
		parserSettings.setHeaderExtractionEnabled(true);
		parserSettings.setDelimiterDetectionEnabled(true, '|');
		CsvParser parser = new CsvParser(parserSettings);
		parser.parse(new File(config.getString(FILENAME)));
		List<String[]> rows = rowProcessor.getRows();
		for(String[] row : rows) {
			preparedStmt.setString(1, row[0].trim());
			preparedStmt.setString(2, row[1].trim());
			preparedStmt.setString(3, row[2].trim());
			preparedStmt.execute();
		}
		connection.close();
		getQuestionsData();
	}
	
	private static void getQuestionsData() throws SQLException {
		String selectByDifficulty = "SELECT difficulty, COUNT(difficulty) AS diff_cnt FROM questions WHERE is_assigned=0 GROUP BY difficulty";
		String selectByTag = "SELECT tag, COUNT(tag) AS tag_cnt FROM questions WHERE is_assigned=0 GROUP BY tag";
		Connection connection = getConnection();
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(selectByDifficulty);
		while(rs.next()) {
			String difficulty = rs.getString("difficulty");
			int diff_cnt = rs.getInt("diff_cnt");
			difficultyCount.put(difficulty, diff_cnt);
		}
		rs = stmt.executeQuery(selectByTag);
		while(rs.next()) {
			String tag = rs.getString("tag");
			int tag_cnt = rs.getInt("tag_cnt");
			tagCount.put(tag, tag_cnt);
		}
		connection.close();
	}
	
	private static void insertDifficultyQues(int quizNo, String difficulty, String tag) throws SQLException {
		String selectQuery = "SELECT qid FROM questions WHERE is_assigned = 0 AND difficulty = ? AND tag = ? LIMIT 2";
		String updateQuery = "UPDATE questions SET is_assigned = 1 where qid = ?";
		String insertQuery = "INSERT INTO quizzes(qz_name, ques_id) VALUES (?, ?)";
		Connection connection = getConnection();
		PreparedStatement selectStmt = connection.prepareStatement(selectQuery);
		PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
		PreparedStatement insertStmt = connection.prepareStatement(insertQuery);
		selectStmt.setString(1, difficulty);
		selectStmt.setString(2, tag);
		ResultSet rs = selectStmt.executeQuery();
		while(rs.next()) {
			int qId = rs.getInt("qid");
			updateStmt.setInt(1, qId);
			updateStmt.execute();
			insertStmt.setString(1, "Quiz"+quizNo);
			insertStmt.setInt(2, qId);
			insertStmt.execute();
			difficultyCount.put(difficulty, difficultyCount.get(difficulty) - 1);
			tagCount.put(tag, tagCount.get(tag) - 1);
		}
		connection.close();
	}
	
	private static void insertTagQuestions(int quizNo, String tag, int limit) throws SQLException {
		String selectQuery = "SELECT qid, difficulty FROM questions WHERE is_assigned = 0 AND tag = ? LIMIT " + limit;
		String updateQuery = "UPDATE questions SET is_assigned = 1 where qid = ?";
		String insertQuery = "INSERT INTO quizzes(qz_name, ques_id) VALUES (?, ?)";
		Connection connection = getConnection();
		PreparedStatement selectStmt = connection.prepareStatement(selectQuery);
		PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
		PreparedStatement insertStmt = connection.prepareStatement(insertQuery);
		selectStmt.setString(1, tag);
		ResultSet rs = selectStmt.executeQuery();
		while(rs.next()) {
			int qId = rs.getInt("qid");
			String difficulty = rs.getString("difficulty");
			updateStmt.setInt(1, qId);
			updateStmt.execute();
			insertStmt.setString(1, "Quiz"+quizNo);
			insertStmt.setInt(2, qId);
			insertStmt.execute();
			difficultyCount.put(difficulty, difficultyCount.get(difficulty) - 1);
			tagCount.put(tag, tagCount.get(tag) - 1);
		}
		connection.close();
	}
	
	private static void checkQuizGenPossibility() {
		int sumOfQues = 0;
		for(String difficulty: difficultyCount.keySet()) {
			if(difficultyCount.get(difficulty) < 2)
				isQuizGenPossible = false;
		}
		for(String tag: tagCount.keySet()) {
			sumOfQues += tagCount.get(tag);
			if(tagCount.get(tag) < 2)
				isQuizGenPossible = false;
		}
		if(sumOfQues < 10) 
			isQuizGenPossible = false;
	}
	
	/*private static void setTagAndDifficulty() {
		tagsList = config.getStringList(TAGS);
		difficultyList = config.getStringList(DIFFICULTY);
	}
	
	private static int getRandTagIdx() {
		return new Random().nextInt(tagsList.size());
	}
	
	private static int getRandDiffIdx() {
		return new Random().nextInt(difficultyList.size());
	}*/
	
	private static Connection getConnection() throws SQLException {
		String host = config.getString(DB_HOSTNAME);
		int port = config.getInt(DB_PORT);
		String username = config.getString(DB_USERNAME);
		String password = config.getString(DB_PASSOWRD);
		String database = config.getString(DB_NAME);
		String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" +database;
		return DriverManager.getConnection(dbUrl, username, password);
    }
	
	public static void main(String[] args) throws Exception {
		try {
			config = ConfigFactory.load();
			createSchema();
			insertFileDataToDB();
			int noOfQuizzes = 0;
			while(isQuizGenPossible) {
				int[] limit = {1, 2};
				insertDifficultyQues(noOfQuizzes + 1, "EASY", "tag1");
				insertDifficultyQues(noOfQuizzes + 1, "MEDIUM", "tag2");
				insertDifficultyQues(noOfQuizzes + 1, "HARD", "tag3");
				insertTagQuestions(noOfQuizzes + 1, "tag4", 1);
				insertTagQuestions(noOfQuizzes + 1, "tag5", 2);
				insertTagQuestions(noOfQuizzes + 1, "tag6", 1);
				checkQuizGenPossibility();
				noOfQuizzes++;
			}
			System.out.println("Max no of Quizzes possible : " + noOfQuizzes);
//			dropTables();
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

}
