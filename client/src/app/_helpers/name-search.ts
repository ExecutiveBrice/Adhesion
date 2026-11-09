export function filterByName<T>(people: T[], query: string, name: (person: T) => string): T[] {
  const normalize = (value: string) => value.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLocaleLowerCase('fr');
  const terms = normalize(query).trim().split(/\s+/).filter(Boolean);
  return people.filter(person => {
    const normalizedName = normalize(name(person));
    return terms.every(term => normalizedName.includes(term));
  });
}
