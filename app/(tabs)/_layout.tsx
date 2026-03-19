import { Tabs } from 'expo-router';
import React from 'react';
import { View, Platform, Dimensions, Pressable } from 'react-native';
import type { BottomTabBarProps } from '@react-navigation/bottom-tabs';

import { HapticTab } from '@/components/haptic-tab';
import { IconSymbol, IconSymbolName } from '@/components/ui/icon-symbol';

function TabIcon({ name, focused, color }: { name: IconSymbolName; focused: boolean; color: string }) {
  return (
    <View
      style={{
        width: 40,
        height: 40,
        borderRadius: 20,
        backgroundColor: focused ? '#B8FF2D' : 'transparent',
        justifyContent: 'center',
        alignItems: 'center',
      }}>
      <IconSymbol size={22} name={name} color={focused ? '#000' : color} />
    </View>
  );
}

export default function TabLayout() {
  return (
    <Tabs
      tabBar={(props) => <CenteredDockTabBar {...props} />}
      screenOptions={{
        headerShown: false,
        tabBarButton: HapticTab,
        tabBarShowLabel: false,
      }}>
      <Tabs.Screen
        name="index"
        options={{
          title: 'Home',
          tabBarIcon: ({ color, focused }) => <TabIcon name="house.fill" focused={focused} color={color} />,
        }}
      />
      <Tabs.Screen
        name="menu"
        options={{
          title: 'Menu',
          tabBarIcon: ({ color, focused }) => <TabIcon name="line.3.horizontal" focused={focused} color={color} />,
        }}
      />
    </Tabs>
  );
}

function CenteredDockTabBar({ state, navigation, descriptors }: BottomTabBarProps) {
  const screenWidth = Dimensions.get('window').width;
  const BAR_WIDTH = 130;
  const BAR_HEIGHT = 58;

  const activeRoute = state.routes[state.index]?.name;
  const isMenuFocused = activeRoute === 'menu';

  const activeBubble = '#B8FF2D';

  return (
    <View
      style={{
        position: 'absolute',
        bottom: Platform.OS === 'ios' ? 24 : 16,
        left: (screenWidth - BAR_WIDTH) / 2,
        width: BAR_WIDTH,
        height: BAR_HEIGHT,
        borderRadius: BAR_HEIGHT / 2,
        backgroundColor: '#1C1C1E',
        borderWidth: 1,
        borderColor: 'rgba(255,255,255,0.12)',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 8 },
        shadowOpacity: 0.4,
        shadowRadius: 12,
        elevation: 8,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
      }}>
      {/* Left primary Home button */}
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={descriptors[state.routes.find((r) => r.name === 'index')?.key ?? state.routes[0].key]?.options?.title ?? 'Home'}
        onPress={() => navigation.navigate('index')}
        style={({ pressed }) => ({
          width: 44,
          height: 44,
          borderRadius: 22,
          backgroundColor: activeBubble,
          alignItems: 'center',
          justifyContent: 'center',
          opacity: pressed ? 0.92 : 1,
          marginRight: 10,
        })}>
        <IconSymbol name="house.fill" size={20} color="#000" />
      </Pressable>

      {/* Right Menu button */}
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={descriptors[state.routes.find((r) => r.name === 'menu')?.key ?? state.routes[state.routes.length - 1].key]?.options?.title ?? 'Menu'}
        onPress={() => navigation.navigate('menu')}
        style={({ pressed }) => ({
          width: 44,
          height: 44,
          alignItems: 'center',
          justifyContent: 'center',
          opacity: pressed ? 0.7 : 1,
        })}>
        <IconSymbol name="line.3.horizontal" size={22} color={isMenuFocused ? activeBubble : '#FFF'} />
      </Pressable>
    </View>
  );
}
