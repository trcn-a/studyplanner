export interface Subject {
    id: string;
    name: string;
    description: string;
    color: string;
}
export type SubjectInput = Omit<Subject, 'id'>;
export interface SubjectEditor {
    id?: string;
    input: SubjectInput;
}
