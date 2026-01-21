import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, Calendar, Wallet, Camera, ChevronRight } from 'lucide-react';
import { Button } from '../ui/button';

interface OnboardingStep {
  icon: React.ReactNode;
  title: string;
  description: string;
  color: string;
}

const onboardingSteps: OnboardingStep[] = [
  {
    icon: <Users className="w-12 h-12" />,
    title: '모임을 만들고 관리하세요',
    description: '동아리, 스터디, 정모 등 다양한 모임을 쉽게 만들고 관리할 수 있어요.',
    color: 'bg-blue-500',
  },
  {
    icon: <Calendar className="w-12 h-12" />,
    title: '일정을 함께 정하세요',
    description: '투표 기능으로 모임 일정을 쉽게 조율하고, 참석 여부를 확인하세요.',
    color: 'bg-green-500',
  },
  {
    icon: <Wallet className="w-12 h-12" />,
    title: '회비를 투명하게 관리하세요',
    description: '수입/지출 내역을 기록하고, 정산 요청으로 편리하게 관리하세요.',
    color: 'bg-purple-500',
  },
  {
    icon: <Camera className="w-12 h-12" />,
    title: '추억을 함께 나누세요',
    description: '스토리 기능으로 모임의 소중한 순간들을 공유하세요.',
    color: 'bg-orange-500',
  },
];

export function WelcomeView() {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(0);

  const handleNext = () => {
    if (currentStep < onboardingSteps.length - 1) {
      setCurrentStep(currentStep + 1);
    } else {
      navigate('/');
    }
  };

  const handleSkip = () => {
    navigate('/');
  };

  const step = onboardingSteps[currentStep];

  return (
    <div className="min-h-screen bg-white flex flex-col">
      {/* Skip Button */}
      <div className="p-4 flex justify-end">
        <button
          onClick={handleSkip}
          className="text-stone-500 hover:text-stone-700 text-sm"
        >
          건너뛰기
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 flex flex-col items-center justify-center p-8">
        {/* Icon */}
        <div 
          className={`w-24 h-24 ${step.color} rounded-3xl flex items-center justify-center text-white mb-8 shadow-lg transition-all duration-500`}
          key={currentStep}
        >
          {step.icon}
        </div>

        {/* Text */}
        <div className="text-center space-y-4 animate-in fade-in duration-500" key={`text-${currentStep}`}>
          <h1 className="text-2xl font-bold text-stone-900">{step.title}</h1>
          <p className="text-stone-500 max-w-xs mx-auto">{step.description}</p>
        </div>
      </div>

      {/* Progress & Button */}
      <div className="p-6 space-y-6">
        {/* Progress Dots */}
        <div className="flex justify-center gap-2">
          {onboardingSteps.map((_, index) => (
            <button
              key={index}
              onClick={() => setCurrentStep(index)}
              className={`w-2 h-2 rounded-full transition-all ${
                index === currentStep 
                  ? 'w-8 bg-orange-500' 
                  : index < currentStep 
                    ? 'bg-orange-300' 
                    : 'bg-stone-200'
              }`}
            />
          ))}
        </div>

        {/* Next Button */}
        <Button
          onClick={handleNext}
          className="w-full h-12 bg-orange-500 hover:bg-orange-600 text-white text-lg font-medium rounded-xl"
        >
          {currentStep < onboardingSteps.length - 1 ? (
            <>
              다음
              <ChevronRight className="w-5 h-5 ml-1" />
            </>
          ) : (
            '시작하기'
          )}
        </Button>
      </div>
    </div>
  );
}

