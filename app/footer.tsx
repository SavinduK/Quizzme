import { FontAwesome5 } from '@expo/vector-icons';
import { usePathname, useRouter } from 'expo-router';
import React from 'react';
import { Pressable, StyleSheet, Text, View, useColorScheme } from 'react-native';
import { Colors } from './theme';

export default function Footer() {
  const router = useRouter();
  const pathname = usePathname();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const isActive = (path: string) => pathname === path;

  return (
    <View style={[styles.footer, { backgroundColor: theme.card, borderColor: theme.border }]}>
      {/* HOME TAB */}
      <Pressable 
        style={styles.tab} 
        onPress={() => router.replace('/')}
      >
        <FontAwesome5 
          name="home" 
          size={18} 
          color={isActive('/') ? theme.accent : theme.subtext} 
        />
        <Text style={[styles.tabText, { color: isActive('/') ? theme.accent : theme.subtext }]}>Home</Text>
      </Pressable>

      {/* PRACTICE TAB */}
      <Pressable 
        style={styles.tab} 
        onPress={() => router.replace('/quiz-session')}
      >
        <FontAwesome5 
          name="graduation-cap" 
          size={18} 
          color={isActive('/quiz-session') ? theme.accent : theme.subtext} 
        />
        <Text style={[styles.tabText, { color: isActive('/quiz-session') ? theme.accent : theme.subtext }]}>Practice</Text>
      </Pressable>

      {/* SETTINGS TAB */}
      <Pressable 
        style={styles.tab} 
        onPress={() => router.replace('/settings')}
      >
        <FontAwesome5 
          name="cog" 
          size={18} 
          color={isActive('/settings') ? theme.accent : theme.subtext} 
        />
        <Text style={[styles.tabText, { color: isActive('/settings') ? theme.accent : theme.subtext }]}>Settings</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: 75,
    flexDirection: 'row',
    borderTopWidth: 1,
    paddingBottom: 15,
    alignItems: 'center',
    justifyContent: 'space-around',
  },
  tab: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 10,
    flex: 1,
  },
  tabText: {
    fontSize: 11,
    fontWeight: '600',
    marginTop: 4,
  },
});