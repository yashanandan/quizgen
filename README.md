# QuizGen

QuizGenerator used to generate quizzes from the given file. This is basic working model that can be used and built upon. NOTE: There are no test cases as it tends to vary on who uses this and how.

## Assumptions Made:
- Each Quiz can have a maximum upto 10 questions
- There are 3 difficulties with respect to the questions : EASY, MEDIUM, HARD
- There are 6 different types of questions : tag1, tag2, tag3, tag4, tag5, tag6
- Each Quiz has 2 questions from each difficulty and atleast 1 from each tag

## Tech Used
- MySQL 5.7
- TypeSafe Config
- Univocity CSV Parser

## DB Settings
- There is a defualt set of setting for the db inorder to use them create a repo `**quiz**`.
- The default settings can be overriden by changing the `application.conf` file sections

## Running the Project 
```
clone git@github.com:yashanandan/quizgen.git
import them into your IDE and run as Java Application with QuizGenerator class as main class
```
