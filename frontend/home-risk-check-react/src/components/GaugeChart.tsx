import { Pie, PieChart, type PieProps, type PieSectorDataItem, Tooltip } from 'recharts';

// #region Sample data
const chartData = [
    { name: 'A', value: 60, fill: 'green' },
    { name: 'B', value: 60, fill: 'orange' },
    { name: 'C', value: 60, fill: 'red' },
];

interface NeedleProps {
    score: number;
}

const NEEDLE_BASE_RADIUS_PX = 5;
const NEEDLE_COLOR = '#111';
const Needle = ({score, cx, cy, innerRadius, outerRadius }: NeedleProps & PieSectorDataItem) => {
    const needleBaseCenterX = cx;
    const needleBaseCenterY = cy;
    const needleLength = innerRadius + (outerRadius - innerRadius);
    const angle = 180 - (score / 100) * 180;

    return (
        <g>
            {/* 바늘 삼각형 */}
            <polygon
                points={`${needleBaseCenterX + needleLength * 0.5}, ${needleBaseCenterY}, ${needleBaseCenterX},${needleBaseCenterY - NEEDLE_BASE_RADIUS_PX}, ${needleBaseCenterX},${needleBaseCenterY + NEEDLE_BASE_RADIUS_PX}`}
                fill={NEEDLE_COLOR}
                style={{transform: `rotate(-${angle}deg)`, transformOrigin: `${needleBaseCenterX}px ${needleBaseCenterY}px`,}}
            />

            {/* 중심 원 */}
            <circle
                cx={needleBaseCenterX}
                cy={needleBaseCenterY}
                r={NEEDLE_BASE_RADIUS_PX}
                fill={NEEDLE_COLOR}
            />
        </g>
    );
};

const HalfPie = (props: PieProps) => (
    <Pie
        {...props}
        stroke="none"
        dataKey="value"
        startAngle={180}
        endAngle={0}
        data={chartData}
        cx={100}
        cy={100}
        innerRadius={60}
        outerRadius={100}
        cornerRadius={5}
        paddingAngle={2}
    />
);

export default function PieChartWithNeedle({score, isAnimationActive = false }: { score:number, isAnimationActive?: boolean }) {
    return (
        <PieChart width={210} height={120}>
            <HalfPie isAnimationActive={isAnimationActive} />
            <HalfPie isAnimationActive={isAnimationActive} shape={(props) => <Needle {...props} score={score} />} />
            <Tooltip defaultIndex={0} content={() => null} active />
        </PieChart>
    );
}