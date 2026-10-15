import { PlannerPage } from '../pages/planner/PlannerPage';
import { AccountGate } from '../features/account/components/AccountGate';
export default function App() { return <AccountGate><PlannerPage /></AccountGate>; }
