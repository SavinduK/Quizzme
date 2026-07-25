import { Stack } from 'expo-router';
import { FilterProvider } from './context/filter-context';

export default function RootLayout() {
  return (
    <FilterProvider>
      <Stack screenOptions={{headerShown:false}}/>;
    </FilterProvider>
  );
}