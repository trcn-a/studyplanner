import { Sparkles } from 'lucide-react';
import type { ModelInfo } from '../model/types';
export function PredictionNote({model}:{model:ModelInfo|null}) {
 if(!model) return null;
 return <p className="prediction-note"><Sparkles size={14}/>{model.samples ? `Історія: ${model.samples} завдань${model.errorPercent !== null ? ` · Похибка прогнозу: ${model.errorPercent}%` : ''}` : 'Для прогнозів потрібна історія виконаних завдань.'}</p>;
}
