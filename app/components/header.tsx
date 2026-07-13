import { FontAwesome5 } from '@expo/vector-icons'; // Or wherever your FontAwesome5 comes from
import React from 'react';
import { Pressable, StyleSheet, Text, View, useColorScheme } from 'react-native';
import { Colors } from '../constants/theme';

interface HeaderProps {
  title: string;
  onRightButtonPress?: () => void; // Optional prop
}

export default function Header({ title,  onRightButtonPress }: HeaderProps) {
  const theme = Colors[useColorScheme() ?? 'light'];
  return (
    <View style={styles.header}>
      <Text style={[styles.headerTitle, { color: theme.title }]}>
        {title}
      </Text>
      
      {/* Only render the button if an action is provided */}
      {onRightButtonPress && (
        <Pressable 
          style={[styles.addBtn, { backgroundColor: theme.background }]} 
          onPress={onRightButtonPress}
        >
          <FontAwesome5 name="plus" size={18} color="white" />
        </Pressable>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 25, paddingTop: 20,marginBottom:5 },
  headerTitle: { fontSize: 24, fontWeight: '800', letterSpacing: -0.5 },
  addBtn: { width: 40, height: 40, borderRadius: 22, justifyContent: 'center', alignItems: 'center' },
});