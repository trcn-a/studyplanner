import { BookOpen, Plus } from 'lucide-react';
export function Welcome({ onCreate }: {
    onCreate: () => void;
}) {
    return <div className="welcome">
    <div className="welcome-icon">
    <BookOpen size={36}/>
    </div>

    <h1>Додай перший предмет</h1>

    <button className="primary" onClick={onCreate}>
    <Plus size={18}/>Створити предмет</button>
    </div>;
}
