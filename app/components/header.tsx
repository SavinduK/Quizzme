import { FontAwesome5 } from '@expo/vector-icons';
import React from 'react';
import { Pressable, StyleSheet, Text, View, useColorScheme } from 'react-native';
import { Colors } from '../constants/theme';

interface HeaderProps {
  title: string;
  onRightButtonPress?: () => void;
}

export default function Header({ title, onRightButtonPress }: HeaderProps) {
  const theme = Colors[useColorScheme() ?? 'light'];

  return (
    <View style={[styles.header,{borderColor:theme.border}]}>
      <Text style={[styles.headerTitle, { color: theme.title }]}>
        {title}
      </Text>
      
      {onRightButtonPress && (
        <Pressable 
          style={styles.addBtn} 
          onPress={onRightButtonPress}
        >
          {/* Changed color from "white" to your custom purple */}
          <FontAwesome5 name="plus" size={12} color="#C084FC" />
        </Pressable>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  header: { 
    flexDirection: 'row', 
    justifyContent: 'space-between', 
    alignItems: 'center', 
    paddingHorizontal: 25, 
    paddingTop: 20,
    paddingBottom: 15, 
    borderBottomWidth:1
  },
  headerTitle: { 
    fontSize: 24, 
    textTransform:'capitalize',
    fontWeight: '800', 
    letterSpacing: -0.5 
  },
  addBtn: { 
    width: 35, 
    height: 35, 
    borderRadius: 8,
    justifyContent: 'center', 
    alignItems: 'center',
    backgroundColor: 'rgba(192, 132, 252, 0.15)', 
    borderWidth: 1,
    borderColor: '#C084FC', 
  },
});