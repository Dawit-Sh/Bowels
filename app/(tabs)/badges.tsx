import React from 'react';
import { StyleSheet, Text, View, ScrollView } from 'react-native';
import { SymbolView } from 'expo-symbols';

import { Card } from '@/components/ui/Card';
import { Screen } from '@/components/ui/Screen';
import { getDb } from '@/db/client';
import { useTheme } from '@/theme/ThemeProvider';

const BADGES_DEFS = [
  { id: 'b1', name: 'First Drop', requirement: 1, desc: 'Tracked your first day.', icon: 'drop.circle.fill' },
  { id: 'b2', name: 'Fiber Champion', requirement: 7, desc: 'Tracked for 7 distinct days.', icon: 'leaf.fill' },
  { id: 'b3', name: 'Gut Master', requirement: 14, desc: 'Tracked for 14 distinct days.', icon: 'star.circle.fill' },
  { id: 'b4', name: 'Microbiome Deity', requirement: 30, desc: 'Tracked for 30 distinct days.', icon: 'crown.fill' },
  { id: 'b5', name: 'Consistency King', requirement: 60, desc: 'Tracked for 60 distinct days.', icon: 'figure.walk.circle.fill' },
  { id: 'b6', name: 'Inner Peace', requirement: 90, desc: 'Tracked for 90 distinct days.', icon: 'sun.max.fill' },
  { id: 'b7', name: 'Hydration Hero', requirement: 120, desc: 'Tracked for 120 distinct days.', icon: 'drop.fill' },
  { id: 'b8', name: 'Gut Sage', requirement: 180, desc: 'Tracked for 180 distinct days.', icon: 'brain.head.profile' },
  { id: 'b9', name: 'Equilibrium', requirement: 270, desc: 'Tracked for 270 distinct days.', icon: 'scale.3d' },
  { id: 'b10', name: '365 Mastery', requirement: 365, desc: 'Tracked for a full year!', icon: 'flame.fill' },
];

export default function BadgesScreen() {
  const theme = useTheme();
  const [days, setDays] = React.useState(0);

  React.useEffect(() => {
    (async () => {
      const db = await getDb();
      const res = await db.getFirstAsync<{ count: number }>('SELECT COUNT(DISTINCT date_key) as count FROM sessions');
      setDays(res?.count ?? 0);
    })();
  }, []);

  return (
    <Screen>
      <Text style={[styles.title, { color: theme.colors.textPrimary }]}>Badges</Text>
      <Text style={{ color: theme.colors.textSecondary, marginBottom: 16 }}>You have tracked exactly {days} distinct days.</Text>
      
      <ScrollView contentContainerStyle={{ gap: 12 }} showsVerticalScrollIndicator={false}>
        {BADGES_DEFS.map((b) => {
          const unlocked = days >= b.requirement;
          return (
            <Card key={b.id} style={{ opacity: unlocked ? 1 : 0.45, flexDirection: 'row', alignItems: 'center', gap: 16 }}>
              <View style={{ width: 50, height: 50, borderRadius: 25, backgroundColor: unlocked ? theme.colors.accent : theme.colors.border, justifyContent: 'center', alignItems: 'center' }}>
                {unlocked ? (
                  <SymbolView name={b.icon as any} tintColor="#fff" size={24} type="monochrome" fallback={<Text>🏅</Text>} />
                ) : (
                  <SymbolView name="lock.fill" tintColor={theme.colors.textSecondary} size={24} type="monochrome" fallback={<Text>🔒</Text>} />
                )}
              </View>
              <View style={{ flex: 1 }}>
                <Text style={{ color: theme.colors.textPrimary, fontWeight: '900', fontSize: 18 }}>{b.name}</Text>
                <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>{b.desc}</Text>
              </View>
            </Card>
          );
        })}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 24, fontWeight: '900', marginBottom: 8 },
});
