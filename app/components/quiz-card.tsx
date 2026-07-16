import { FontAwesome5 } from '@expo/vector-icons';
import React from 'react';
import { Pressable, StyleSheet, Text, TextInput, useColorScheme, View } from 'react-native';
import { Colors } from '../constants/theme';

interface QuizCardProps {
  item: any;
  chosenAnswer: string | null;
  setChosenAnswer: (ans: string | null) => void;
  setRunningScore: React.Dispatch<React.SetStateAction<number>>;
  runningScore: number; 
  maxPossibleScore: number; 
  tfSelections: Record<number, boolean | null>;
  setTfSelections: React.Dispatch<React.SetStateAction<Record<number, boolean | null>>>;
  tfChecked: boolean;
  tfQuestionScore: number;
  evaluateTfQuestion: () => void;
  handleNextQuestion: () => void;
  currentQuestionIdx: number;
  totalQuestions: number;
  
  // Short Answer Props passed down from QuestionSession container
  isSAQuiz?: boolean;
  saInputText?: string;
  setSaInputText?: (text: string) => void;
  saChecked?: boolean;
  setSaChecked?: (checked: boolean) => void;
}

export default function QuizCard({
  item, chosenAnswer, setChosenAnswer, setRunningScore, runningScore, maxPossibleScore,
  tfSelections, setTfSelections, tfChecked, tfQuestionScore, evaluateTfQuestion,
  handleNextQuestion, currentQuestionIdx, totalQuestions,
  isSAQuiz = false, saInputText = "", setSaInputText, saChecked = false, setSaChecked
}: QuizCardProps) {
  const isTFStyle = Array.isArray(item.statements);
  const theme = Colors[useColorScheme() ?? 'light'];

  const progressPercent = totalQuestions > 0 
    ? ((currentQuestionIdx + 1) / totalQuestions) * 100 
    : 0;

  // Handles marking Short Answer as complete
  const checkShortAnswer = () => {
    if (setSaChecked) {
      setSaChecked(true);
    }
  };

  // Handles awarding points during short answer assessment
  const handleGradeSA = (isCorrect: boolean) => {
    if (isCorrect) {
      setRunningScore(p => p + 1);
    }
    handleNextQuestion();
  };

  return (
    <View style={[styles.quizCard, { backgroundColor: theme.card, borderColor: theme.border }]}>
      
      {/* HEADER SECTION (Progress Bar & Score Badge) */}
      <View style={styles.quizHeaderRow}> 
        <View style={[styles.progressBarContainer, { backgroundColor: theme.border }]}>
          <View 
            style={[
              styles.progressBarFill, 
              { 
                width: `${progressPercent}%`, 
                backgroundColor: theme.accent 
              }
            ]} 
          />
        </View>

        <View style={styles.scoreBadge}>
          <Text style={[styles.scoreBadgeText, { color: theme.accent }]}>
             {runningScore} / {maxPossibleScore}
          </Text>
        </View>
      </View>

      {/* QUESTION SECTION */}
      <Text style={[styles.quizQuestion, { color: theme.title }]}>
        {currentQuestionIdx + 1}. {item.question}
      </Text>
      
      {/* 1. STANDARD MCQ */}
      {!isTFStyle && !isSAQuiz && item.options?.map((option: string, oIdx: number) => {
        const isSelected = chosenAnswer === option;
        const isCorrect = option === item.correct_answer;
        let optionBg = 'transparent';
        let optionBorder = theme.border;

        if (chosenAnswer) {
          if (isCorrect) { optionBg = 'rgba(74, 222, 128, 0.15)'; optionBorder = '#4ade80'; }
          else if (isSelected) { optionBg = 'rgba(248, 113, 113, 0.15)'; optionBorder = '#f87171'; }
        }

        return (
          <Pressable
            key={oIdx}
            style={[styles.optionButton, { backgroundColor: optionBg, borderColor: optionBorder }]}
            onPress={() => {
              if (chosenAnswer === option) {
                setChosenAnswer(null);
                if (isCorrect) setRunningScore(p => Math.max(0, p - 1));
              } else if (!chosenAnswer) {
                setChosenAnswer(option);
                if (isCorrect) setRunningScore(p => p + 1);
              }
            }}
          >
            <Text style={[styles.optionText, { color: theme.title, fontWeight: isSelected ? '700' : '400' }]}>{oIdx+1}. {option}</Text>
            {chosenAnswer && isCorrect && <FontAwesome5 name="check-circle" size={14} color="#4ade80" />}
            {chosenAnswer && isSelected && !isCorrect && <FontAwesome5 name="times-circle" size={14} color="#f87171" />}
          </Pressable>
        );
      })}

      {/* 2. TRUE / FALSE MULTI-STATEMENT */}
      {isTFStyle && !isSAQuiz && item.statements?.map((statement: string, sIdx: number) => {
        const currentSelection = tfSelections[sIdx];
        const correctBool = item.answers?.[sIdx];
        let statementBorder = theme.border;
        let statementBg = 'transparent';

        if (tfChecked) {
          statementBg = currentSelection === correctBool ? 'rgba(74, 222, 128, 0.08)' : 'rgba(248, 113, 113, 0.08)';
          statementBorder = currentSelection === correctBool ? '#4ade80' : '#f87171';
        }

        return (
          <View key={sIdx} style={[styles.tfStatementRow, { borderColor: statementBorder, backgroundColor: statementBg }]}>
            <View style={{ flex: 1, paddingRight: 4 }}>
              <Text style={[styles.tfStatementText, { color: theme.title }]}>{sIdx + 1}. {statement}</Text>
              {tfChecked && (
                <Text style={[styles.tfFeedbackText, { color: currentSelection === correctBool ? '#4ade80' : '#f87171' }]}>
                  {`(Selected: ${currentSelection === null ? 'None' : (currentSelection ? 'T' : 'F')} | Correct: ${correctBool ? 'T' : 'F'})`}
                </Text>
              )}
            </View>
            <View style={styles.tfActionToggleGroup}>
              <Pressable 
                disabled={tfChecked}
                style={[styles.tfToggleBtn, currentSelection === true && { backgroundColor: theme.accent }]}
                onPress={() => setTfSelections(p => ({ ...p, [sIdx]: currentSelection === true ? null : true }))}
              >
                <Text style={{ fontSize: 11, fontWeight: '700', color: currentSelection === true ? '#fff' : theme.title }}>T</Text>
              </Pressable>
              <Pressable 
                disabled={tfChecked}
                style={[styles.tfToggleBtn, currentSelection === false && { backgroundColor: theme.delete }]}
                onPress={() => setTfSelections(p => ({ ...p, [sIdx]: currentSelection === false ? null : false }))}
              >
                <Text style={{ fontSize: 11, fontWeight: '700', color: currentSelection === false ? '#fff' : theme.title }}>F</Text>
              </Pressable>
            </View>
          </View>
        );
      })}

      {/* 3. SHORT ANSWER SECTION */}
      {isSAQuiz && (
        <View style={styles.saContainer}>
          <TextInput
            style={[styles.saTextInput, { color: theme.title, borderColor: theme.border, backgroundColor: theme.background }]}
            placeholder="Type your answer explanation here..."
            placeholderTextColor={theme.subtext}
            multiline
            numberOfLines={3}
            value={saInputText}
            onChangeText={setSaInputText}
            editable={!saChecked}
          />

          {saChecked && (
            <View style={[styles.saReviewCard, { backgroundColor: theme.background, borderColor: theme.border }]}>
              <Text style={[styles.saLabelText, { color: theme.accent }]}>Correct Key/Term:</Text>
              <Text style={[styles.saValueText, { color: theme.title }]}>{item.correct_answer}</Text>
              
              {item.explanation && (
                <>
                  <Text style={[styles.saLabelText, { color: theme.accent, marginTop: 10 }]}>Context Explanation:</Text>
                  <Text style={[styles.saExplanationText, { color: theme.title }]}>{item.explanation}</Text>
                </>
              )}
            </View>
          )}
        </View>
      )}

      {/* FOOTER ACTIONS */}
      {isTFStyle && tfChecked && !isSAQuiz && (
        <View style={[styles.tfQuestionScoreBadge, { backgroundColor: theme.background }]}>
          <Text style={[styles.tfQuestionScoreText, { color: theme.title }]}>
            Question Marks: <Text style={{ color: theme.accent, fontWeight: '800' }}>+{tfQuestionScore} / 5</Text>
          </Text>
        </View>
      )}

      {isTFStyle && !tfChecked && !isSAQuiz && (
        <Pressable style={[styles.submitActionBtn, { backgroundColor: theme.buttons, borderWidth: 1, borderColor: theme.accent }]} onPress={evaluateTfQuestion}>
          <Text style={[styles.submitActionBtnText, { color: theme.accent }]}>Check Statements</Text>
        </Pressable>
      )}

      {isSAQuiz && !saChecked && (
        <Pressable style={[styles.submitActionBtn, { backgroundColor: theme.buttons, borderWidth: 1, borderColor: theme.accent }]} onPress={checkShortAnswer}>
          <Text style={[styles.submitActionBtnText, { color: theme.accent }]}>Reveal Solution</Text>
        </Pressable>
      )}

      {/* Next controls for MCQ / TF */}
      {((!isTFStyle && !isSAQuiz && chosenAnswer) || (isTFStyle && !isSAQuiz && tfChecked)) && (
        <Pressable style={[styles.submitActionBtn, { backgroundColor: theme.buttons, borderColor: theme.accent, borderWidth: 1 }]} onPress={handleNextQuestion}>
          <Text style={[styles.submitActionBtnText, { color: theme.accent }]}>
            {currentQuestionIdx + 1 === totalQuestions ? "View Final Results" : "Next Question"}
          </Text>
          <FontAwesome5 name="arrow-right" size={12} color={theme.accent} style={{ marginLeft: 8 }} />
        </Pressable>
      )}

      {/* Grading next steps for Short Answer */}
      {isSAQuiz && saChecked && (
        <View style={styles.saSelfGradeRow}>
          <Pressable 
            style={[styles.saGradeBtn, { backgroundColor: 'rgba(248, 113, 113, 0.15)', borderColor: '#f87171' }]} 
            onPress={() => handleGradeSA(false)}
          >
            <FontAwesome5 name="times" size={14} color="#f87171" style={{ marginRight: 6 }} />
            <Text style={[styles.saGradeBtnText, { color: '#f87171' }]}>Incorrect</Text>
          </Pressable>

          <Pressable 
            style={[styles.saGradeBtn, { backgroundColor: 'rgba(74, 222, 128, 0.15)', borderColor: '#4ade80' }]} 
            onPress={() => handleGradeSA(true)}
          >
            <FontAwesome5 name="check" size={14} color="#4ade80" style={{ marginRight: 6 }} />
            <Text style={[styles.saGradeBtnText, { color: '#4ade80' }]}>Correct</Text>
          </Pressable>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  quizCard: { padding: 20, borderRadius: 22, borderWidth: 1, marginBottom: 16, marginTop: 12 },
  quizHeaderRow: { 
    flexDirection: 'row', 
    alignItems: 'center', 
    justifyContent: 'space-between', 
    marginBottom: 18, 
    width: '100%', 
    gap: 12 
  },
  progressBarContainer: { 
    flex: 1, 
    height: 6, 
    borderRadius: 3, 
    overflow: 'hidden' 
  },
  progressBarFill: { height: '100%', borderRadius: 3 },
  scoreBadge: { justifyContent: 'center' },
  scoreBadgeText: { fontSize: 13, fontWeight: '700' },
  quizQuestion: { fontSize: 16, fontWeight: '700', marginBottom: 18, lineHeight: 22 },
  optionButton: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 14, borderRadius: 14, borderWidth: 1, marginBottom: 8 },
  optionText: { fontSize: 14, flex: 1, paddingRight: 10 },
  tfStatementRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 12, borderRadius: 14, borderWidth: 1, marginBottom: 10, gap: 10 },
  tfStatementText: { fontSize: 13, lineHeight: 18 },
  tfFeedbackText: { fontSize: 11, fontWeight: '600', marginTop: 3 },
  tfActionToggleGroup: { flexDirection: 'row', gap: 6 },
  tfToggleBtn: { width: 28, height: 28, borderRadius: 8, backgroundColor: 'rgba(0,0,0,0.04)', justifyContent: 'center', alignItems: 'center' },
  tfQuestionScoreBadge: { width: '100%', padding: 12, borderRadius: 12, alignItems: 'center', marginTop: 6, marginBottom: 2 },
  tfQuestionScoreText: { fontSize: 13, fontWeight: '600' },
  submitActionBtn: { height: 48, borderRadius: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', marginTop: 14 },
  submitActionBtnText: { fontWeight: '700', fontSize: 14 },
  
  // Short Answer Specific Styles
  saContainer: { marginTop: 4, width: '100%' },
  saTextInput: { borderWidth: 1, borderRadius: 14, padding: 14, textAlignVertical: 'top', fontSize: 14, minHeight: 80, marginBottom: 10 },
  saReviewCard: { padding: 14, borderRadius: 14, borderWidth: 1, marginTop: 4, width: '100%' },
  saLabelText: { fontSize: 12, fontWeight: '700', marginBottom: 4, textTransform: 'uppercase', letterSpacing: 0.5 },
  saValueText: { fontSize: 15, fontWeight: '600', marginBottom: 6 },
  saExplanationText: { fontSize: 14, lineHeight: 20 },
  saSelfGradeRow: { flexDirection: 'row', gap: 12, marginTop: 16, width: '100%' },
  saGradeBtn: { flex: 1, height: 48, borderRadius: 14, borderWidth: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  saGradeBtnText: { fontWeight: '700', fontSize: 14 }
});