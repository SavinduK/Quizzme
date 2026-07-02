import { FontAwesome5 } from '@expo/vector-icons';
import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

interface QuizRendererProps {
  questions: {
    mcqs: any[];
    shortAnswers: any[];
    essays: any[];
  };
  state: {
    mcqAnswers: { [key: string]: string };
    revealedShort: { [key: string]: boolean };
    revealedEssays: { [key: string]: boolean };
  };
  onMcqSelect: (qIdx: number, selection: string) => void;
  onToggleShort: (qIdx: number) => void;
  onToggleEssay: (qIdx: number) => void;
  theme: any;
}

export function UnifiedQuizDeck({ questions, state, onMcqSelect, onToggleShort, onToggleEssay, theme }: QuizRendererProps) {
  return (
    <View style={{ gap: 20 }}>
      {/* 1. MCQ SECTION */}
      {questions.mcqs.length > 0 && (
        <View>
          <Text style={[styles.sectionHeading, { color: theme.accent }]}>Multiple Choice</Text>
          {questions.mcqs.map((item, qIdx) => (
            <View key={`mcq-${qIdx}`} style={[styles.card, { backgroundColor: theme.card, borderColor: theme.border }]}>
              <Text style={[styles.questionText, { color: theme.title }]}>{qIdx + 1}. {item.question}</Text>
              {item.options.map((opt: string, oIdx: number) => {
                const userSelection = state.mcqAnswers[qIdx];
                const hasAnswered = userSelection !== undefined;
                const isSelected = userSelection === opt;
                const isCorrect = opt === item.correct_answer;

                let bg = theme.background;
                let border = theme.border;

                if (isSelected) {
                  bg = isCorrect ? theme.success : theme.delete;
                  border = isCorrect ? theme.success : theme.delete;
                } else if (hasAnswered && isCorrect) {
                  bg = theme.success;
                  border = theme.success;
                }

                return (
                  <Pressable
                    key={oIdx}
                    disabled={hasAnswered}
                    style={[styles.optionBtn, { backgroundColor: bg, borderColor: border }]}
                    onPress={() => onMcqSelect(qIdx, opt)}
                  >
                    <Text style={{ color: isSelected || (hasAnswered && isCorrect) ? 'white' : theme.title, fontWeight: '500' }}>{opt}</Text>
                  </Pressable>
                );
              })}
              {state.mcqAnswers[qIdx] !== undefined && item.rationale && (
                <Text style={[styles.rationale, { color: theme.subtext }]}>
                  <Text style={{ fontWeight: '700' }}>Rationale: </Text>{item.rationale}
                </Text>
              )}
            </View>
          ))}
        </View>
      )}

      {/* 2. SHORT ANSWER SECTION */}
      {questions.shortAnswers.length > 0 && (
        <View>
          <Text style={[styles.sectionHeading, { color: theme.accent }]}>Short Answers</Text>
          {questions.shortAnswers.map((item, qIdx) => (
            <View key={`short-${qIdx}`} style={[styles.card, { backgroundColor: theme.card, borderColor: theme.border }]}>
              <Text style={[styles.questionText, { color: theme.title }]}>{qIdx + 1}. {item.question}</Text>
              
              <Pressable 
                style={[styles.revealBtn, { backgroundColor: theme.background, borderColor: theme.border }]} 
                onPress={() => onToggleShort(qIdx)}
              >
                <Text style={{ color: theme.title, fontWeight: '600' }}>
                  {state.revealedShort[qIdx] ? "Hide Answer Key" : "Show Answer Key"}
                </Text>
                <FontAwesome5 name={state.revealedShort[qIdx] ? "eye-slash" : "eye"} size={14} color={theme.subtext} />
              </Pressable>

              {state.revealedShort[qIdx] && (
                <View style={[styles.answerBox, { backgroundColor: theme.background, borderColor: theme.success }]}>
                  <Text style={{ color: theme.title, fontWeight: '500' }}>{item.answer}</Text>
                </View>
              )}
            </View>
          ))}
        </View>
      )}

      {/* 3. STRUCTURED ESSAYS SECTION */}
      {questions.essays.length > 0 && (
        <View>
          <Text style={[styles.sectionHeading, { color: theme.accent }]}>Structured Essays</Text>
          {questions.essays.map((item, qIdx) => (
            <View key={`essay-${qIdx}`} style={[styles.card, { backgroundColor: theme.card, borderColor: theme.border }]}>
              <Text style={[styles.scenarioText, { color: theme.title }]}>Case/Scenario {qIdx + 1}: {item.scenario_or_title}</Text>
              
              {item.parts?.map((p: any, pIdx: number) => (
                <View key={pIdx} style={styles.partContainer}>
                  <Text style={[styles.partLabel, { color: theme.accent }]}>Part ({p.part})</Text>
                  <Text style={[styles.questionText, { color: theme.title, marginBottom: 8 }]}>{p.question}</Text>
                  
                  <Pressable 
                    style={[styles.revealBtn, { backgroundColor: theme.background, borderColor: theme.border, paddingVertical: 8 }]} 
                    onPress={() => onToggleEssay(`${qIdx}-${pIdx}`)}
                  >
                    <Text style={{ fontSize: 13, color: theme.title, fontWeight: '600' }}>
                      {state.revealedEssays[`${qIdx}-${pIdx}`] ? "Hide Marking Guide" : "Check Marking Guide"}
                    </Text>
                  </Pressable>

                  {state.revealedEssays[`${qIdx}-${pIdx}`] && (
                    <View style={[styles.answerBox, { backgroundColor: theme.background, borderColor: theme.success }]}>
                      <Text style={{ color: theme.title, fontSize: 14 }}>{p.answer}</Text>
                    </View>
                  )}
                </View>
              ))}
            </View>
          ))}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  sectionHeading: { fontSize: 14, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 12, marginLeft: 5 },
  card: { padding: 20, borderRadius: 24, marginBottom: 15, borderWidth: 1 },
  questionText: { fontSize: 16, fontWeight: '700', marginBottom: 15, lineHeight: 22 },
  scenarioText: { fontSize: 16, fontWeight: '800', marginBottom: 15, lineHeight: 22 },
  optionBtn: { padding: 14, borderRadius: 14, borderWidth: 1, marginBottom: 8 },
  rationale: { marginTop: 12, fontSize: 13, fontStyle: 'italic', lineHeight: 18 },
  revealBtn: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 14, borderRadius: 14, borderWidth: 1 },
  answerBox: { padding: 14, borderRadius: 14, borderWidth: 1, marginTop: 10, borderLeftWidth: 4 },
  partContainer: { marginTop: 15, paddingTop: 15, borderTopWidth: 1, borderTopColor: 'rgba(0,0,0,0.05)' },
  partLabel: { fontSize: 11, fontWeight: '700', textTransform: 'uppercase', marginBottom: 4 }
});