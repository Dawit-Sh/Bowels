import { useRouter } from 'expo-router';
import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { Card } from '@/components/ui/Card';
import { IconSymbol, type IconSymbolName } from '@/components/ui/icon-symbol';
import { Screen } from '@/components/ui/Screen';
import { useTheme } from '@/theme/ThemeProvider';
import { GutHero } from '@/components/illustrations/GutHero';
import { withAlpha } from '@/utils/color';

export default function MenuScreen() {
  const theme = useTheme();
  const router = useRouter();

  return (
    <Screen contentStyle={{ paddingBottom: 120 }}>
      <View style={{ flex: 1, paddingTop: 16 }}>
        <Text style={[styles.headline, { color: theme.colors.textPrimary }]}>Menu</Text>
        <Text style={{ color: theme.colors.textSecondary, fontWeight: '800', marginTop: 4, textTransform: 'uppercase', letterSpacing: 0.5 }}>
          Shortcuts & Settings
        </Text>
      </View>

      <View style={{ position: 'absolute', opacity: 0.1, right: -120, top: -80, zIndex: -1 }}>
        <GutHero style={{ width: 400, height: 400 }} />
      </View>

      <View style={{ gap: 14, marginTop: 24, zIndex: 10 }}>
        <MenuSection title="Explore" hint="Your data and patterns">
          <MenuRow icon="clock.fill" title="History" subtitle="Review and edit past sessions" onPress={() => router.push('/history')} />
          <MenuRow icon="chart.bar.fill" title="Analytics" subtitle="Trends, frequency, and charts" onPress={() => router.push('/analytics')} />
          <MenuRow icon="sparkles" title="Insights" subtitle="Suggestions and gut optimizations" onPress={() => router.push('/insights')} />
        </MenuSection>

        <MenuSection title="Progress" hint="Motivation and milestones">
          <MenuRow icon="crown.fill" title="Badges" subtitle="Achievements and streak goals" onPress={() => router.push('/badges')} />
          <MenuRow icon="flame.fill" title="Weekly Wrapped" subtitle="Your weekly summary report" onPress={() => router.push('/wrapped')} />
        </MenuSection>

        <MenuSection title="Tools" hint="Extra features">
          <MenuRow icon="bolt.fill" title="Predictions" subtitle="Estimate your rhythm window" onPress={() => router.push('/predictions')} />
          <MenuRow icon="person.circle" title="Health info" subtitle="What we track and why" onPress={() => router.push('/health-info')} />
        </MenuSection>

        <MenuSection title="Preferences" hint="App configuration">
          <MenuRow icon="gearshape.fill" title="Settings" subtitle="Notifications, theme, and defaults" onPress={() => router.push('/settings')} />
        </MenuSection>
      </View>
    </Screen>
  );
}

function MenuSection({
  title,
  hint,
  children,
}: {
  title: string;
  hint: string;
  children: React.ReactNode;
}) {
  const theme = useTheme();
  return (
    <Card style={{ borderRadius: 24, padding: 18 }}>
      <View style={{ marginBottom: 10 }}>
        <Text style={{ color: theme.colors.textPrimary, fontWeight: '900', fontSize: 16, letterSpacing: 0.2 }}>{title}</Text>
        <Text style={{ color: theme.colors.textSecondary, fontWeight: '700', marginTop: 2 }}>{hint}</Text>
      </View>
      <View style={{ gap: 8 }}>{children}</View>
    </Card>
  );
}

function MenuRow({
  icon,
  title,
  subtitle,
  onPress,
}: {
  icon: IconSymbolName;
  title: string;
  subtitle: string;
  onPress: () => void;
}) {
  const theme = useTheme();
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [
        styles.row,
        {
          backgroundColor: pressed ? withAlpha(theme.colors.accent, theme.mode === 'dark' ? 0.10 : 0.08) : theme.colors.surface2,
          borderColor: pressed ? withAlpha(theme.colors.accent, 0.55) : theme.colors.border,
        },
      ]}>
      <View
        style={[
          styles.rowIconWrap,
          { backgroundColor: withAlpha(theme.colors.accent, theme.mode === 'dark' ? 0.16 : 0.14), borderColor: withAlpha(theme.colors.accent, 0.3) },
        ]}>
        <IconSymbol name={icon} size={18} color={theme.colors.accent} />
      </View>
      <View style={{ flex: 1, gap: 2 }}>
        <Text style={{ color: theme.colors.textPrimary, fontWeight: '900', fontSize: 16 }}>{title}</Text>
        <Text style={{ color: theme.colors.textSecondary, fontWeight: '700' }}>{subtitle}</Text>
      </View>
      <View
        style={[
          styles.chevronWrap,
          { backgroundColor: withAlpha(theme.colors.accent, theme.mode === 'dark' ? 0.12 : 0.10) },
        ]}>
        <IconSymbol name="chevron.right" size={20} color={theme.colors.textSecondary} />
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  headline: { fontSize: 36, fontWeight: '900', letterSpacing: -1 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 14,
    borderRadius: 18,
    borderWidth: 1,
  },
  rowIconWrap: {
    width: 38,
    height: 38,
    borderRadius: 14,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  chevronWrap: {
    width: 34,
    height: 34,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
