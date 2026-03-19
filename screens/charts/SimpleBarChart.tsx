import React from 'react';
import { View } from 'react-native';
import { VictoryAxis, VictoryBar, VictoryChart } from 'victory-native';

export function SimpleBarChart({
  data,
  accent,
  labelColor,
}: {
  data: { x: string; y: number }[];
  accent: string;
  labelColor: string;
}) {
  if (!data.length) return <View style={{ height: 180 }} />;

  return (
    <View style={{ height: 180, width: '100%' }}>
      <VictoryChart
        padding={{ top: 18, left: 46, right: 16, bottom: 40 }}
        domainPadding={{ x: 10, y: 10 }}>
        <VictoryAxis
          style={{
            axis: { stroke: 'transparent' },
            ticks: { stroke: 'transparent' },
            tickLabels: { fill: labelColor, fontSize: 10, fontWeight: '700' },
          }}
        />
        <VictoryAxis
          dependentAxis
          style={{
            axis: { stroke: 'transparent' },
            grid: { stroke: 'rgba(0,0,0,0.08)' },
            ticks: { stroke: 'transparent' },
            tickLabels: { fill: labelColor, fontSize: 10, fontWeight: '700' },
          }}
        />
        <VictoryBar
          data={data}
          style={{
            data: { fill: accent, width: 12 },
          }}
          cornerRadius={{ top: 6, bottom: 6 }}
          animate={{ duration: 350 }}
        />
      </VictoryChart>
    </View>
  );
}

