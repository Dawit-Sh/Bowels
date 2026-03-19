import { useRouter } from 'expo-router';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Screen } from '@/components/ui/Screen';
import { useTheme } from '@/theme/ThemeProvider';
import { BRISTOL } from '@/utils/healthInfo';

export function HealthInfoScreen() {
  const theme = useTheme();
  const router = useRouter();

  return (
    <Screen>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
        <Text style={[styles.title, { color: theme.colors.textPrimary }]}>Health Info</Text>
        <Button label="Close" variant="ghost" onPress={() => router.back()} />
      </View>
      <Text style={[styles.subtitle, { color: theme.colors.textSecondary }]}>
        Lightweight offline guidance (not medical advice).
      </Text>

      <Card>
        <Text style={{ color: theme.colors.textPrimary, fontWeight: '900', marginBottom: 8 }}>Bristol stool chart</Text>
        <View style={{ gap: 8 }}>
          {BRISTOL.map((b) => (
            <View key={b.type} style={{ flexDirection: 'row', justifyContent: 'space-between', gap: 12 }}>
              <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>Type {b.type}</Text>
              <Text style={{ color: theme.colors.textSecondary, fontWeight: '700', flex: 1, textAlign: 'right' }}>
                {b.label}
              </Text>
            </View>
          ))}
        </View>
      </Card>

      <Card>
        <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>Healthy bowel patterns</Text>
        <Text style={{ marginTop: 8, color: theme.colors.textSecondary, fontWeight: '700' }}>
          Many people fall anywhere between 3 times per day and 3 times per week. What matters most is your personal
          baseline and changes from it.
        </Text>
      </Card>

      <Card>
        <Text style={{ color: theme.colors.textPrimary, fontWeight: '900' }}>When to seek medical help</Text>
        <Text style={{ marginTop: 8, color: theme.colors.textSecondary, fontWeight: '700' }}>
          Seek care if you have persistent or severe abdominal pain, ongoing rectal bleeding, black/tarry stools,
          fever, dehydration, or a sudden unexplained change in bowel habits.
        </Text>
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 22, fontWeight: '900' },
  subtitle: { fontSize: 12, fontWeight: '700' },
});

