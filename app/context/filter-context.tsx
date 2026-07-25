import React, { createContext, ReactNode, useContext, useState } from 'react';

interface FilterContextType {
  selectedSubject: string | null;
  setSelectedSubject: (subject: string | null) => void;
  selectedTerm: string | null;
  setSelectedTerm: (term: string | null) => void;
  resetFilters: () => void;
}

const FilterContext = createContext<FilterContextType | undefined>(undefined);

export const FilterProvider = ({ children }: { children: ReactNode }) => {
  const [selectedSubject, setSelectedSubject] = useState<string | null>(null);
  const [selectedTerm, setSelectedTerm] = useState<string | null>(null);

  const resetFilters = () => {
    setSelectedSubject(null);
    setSelectedTerm(null);
  };

  return (
    <FilterContext.Provider
      value={{
        selectedSubject,
        setSelectedSubject,
        selectedTerm,
        setSelectedTerm,
        resetFilters,
      }}
    >
      {children}
    </FilterContext.Provider>
  );
};

export const useModuleFilters = () => {
  const context = useContext(FilterContext);
  if (!context) {
    throw new Error('useModuleFilters must be used within a FilterProvider');
  }
  return context;
};