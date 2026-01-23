import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { MessageCircle, X, Bot, User, Send, Loader2 } from 'lucide-react';
import { Button } from '../../ui/button';
import { Card, CardContent, CardHeader } from '../../ui/card';
import { Input } from '../../ui/input';
import { askAI } from '../../../../api/ai';
import { toast } from 'sonner';
import { getMyInfo } from '../../../../api/user';

interface QAPair {
  question: string;
  answer: string;
}

interface QAChatWidgetProps {
  groupId?: number;
}

export function QAChatWidget({ groupId }: QAChatWidgetProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [message, setMessage] = useState('');
  const [chatHistory, setChatHistory] = useState<QAPair[]>([]);
  const [isAskingAI, setIsAskingAI] = useState(false);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);

  // 현재 사용자 ID 조회
  useEffect(() => {
    async function fetchUserId() {
      try {
        const userInfo = await getMyInfo();
        setCurrentUserId(userInfo.userId);
      } catch (error) {
        console.error('사용자 정보 조회 실패:', error);
      }
    }
    fetchUserId();
  }, []);

  const handleAskAI = async () => {
    if (!message.trim() || !groupId || !currentUserId) {
      if (!groupId) {
        toast.error('모임 정보를 찾을 수 없습니다');
      }
      return;
    }

    const question = message.trim();
    setMessage('');
    
    try {
      setIsAskingAI(true);
      const response = await askAI(groupId, currentUserId, question);
      
      // 채팅 히스토리에 추가
      setChatHistory(prev => [...prev, { question, answer: response.answer }]);
    } catch (error: any) {
      console.error('AI 질문 실패:', error);
      console.error('에러 상세:', {
        message: error?.message,
        status: error?.status,
        response: error?.response,
        stack: error?.stack
      });
      
      // 에러 메시지 추출
      let errorMessage = '죄송합니다. AI 서비스에 문제가 발생했습니다.';
      if (error?.message) {
        errorMessage = error.message;
      } else if (error?.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (typeof error === 'string') {
        errorMessage = error;
      }
      
      // 상태 코드가 있는 경우 추가 정보 제공
      if (error?.status) {
        if (error.status === 500) {
          errorMessage = '서버에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.';
        } else if (error.status === 404) {
          errorMessage = 'AI 서비스를 찾을 수 없습니다.';
        } else if (error.status === 401 || error.status === 403) {
          errorMessage = '인증이 필요합니다. 다시 로그인해주세요.';
        }
      }
      
      toast.error(`AI 질문에 실패했습니다: ${errorMessage}`);
      
      // 에러 메시지도 히스토리에 추가
      setChatHistory(prev => [...prev, { 
        question, 
        answer: errorMessage
      }]);
    } finally {
      setIsAskingAI(false);
    }
  };

  return (
    <>
      {/* Q&A 버튼 - 항상 표시 */}
      <div className="relative">
        <Button
          onClick={() => setIsExpanded(!isExpanded)}
          className="rounded-full h-14 w-14 shadow-lg bg-orange-500 hover:bg-orange-600 text-white p-0"
          aria-label={isExpanded ? "Q&A 닫기" : "Q&A 열기"}
        >
          {isExpanded ? <X className="w-6 h-6" /> : <MessageCircle className="w-6 h-6" />}
        </Button>
      </div>
      
      {/* 채팅 UI - 펼쳐졌을 때만 표시 */}
      {isExpanded && typeof document !== 'undefined' && createPortal(
        <Card className="fixed top-24 left-4 w-[420px] h-[600px] max-h-[calc(100vh-140px)] shadow-2xl border-2 border-orange-200 flex flex-col z-[9999] bg-white">
          <CardHeader className="flex flex-row items-center justify-between pb-3 border-b border-stone-100">
            <div className="flex items-center gap-2">
              <Bot className="w-5 h-5 text-orange-500" />
              <h3 className="font-semibold text-stone-900">Q&A</h3>
            </div>
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={() => setIsExpanded(false)}
              aria-label="Q&A 닫기"
            >
              <X className="w-4 h-4" />
            </Button>
          </CardHeader>
          <CardContent className="flex-1 overflow-y-auto p-4 space-y-4">
            {chatHistory.length === 0 && !isAskingAI && (
              <div className="text-center text-stone-500 py-8">
                <Bot className="w-12 h-12 mx-auto mb-2 text-stone-300" />
                <p>게시글과 관련된 질문을 해보세요</p>
                <p className="text-xs mt-1">예: "최근 게시글에 대해 알려줘"</p>
              </div>
            )}
            
            {chatHistory.map((qa, index) => (
              <div key={index} className="space-y-2">
                {/* 사용자 질문 (오른쪽 정렬) */}
                <div className="flex justify-end">
                  <div className="max-w-[80%] bg-orange-500 text-white rounded-2xl rounded-tr-sm px-4 py-2">
                    <div className="flex items-start gap-2">
                      <User className="w-4 h-4 mt-0.5 flex-shrink-0" />
                      <p className="text-sm font-medium">{qa.question}</p>
                    </div>
                  </div>
                </div>
                {/* AI 답변 (왼쪽 정렬) */}
                <div className="flex justify-start">
                  <div className="max-w-[80%] bg-stone-100 text-stone-900 rounded-2xl rounded-tl-sm px-4 py-2">
                    <div className="flex items-start gap-2">
                      <Bot className="w-4 h-4 mt-0.5 flex-shrink-0 text-orange-500" />
                      <p className="text-sm whitespace-pre-wrap">{qa.answer}</p>
                    </div>
                  </div>
                </div>
              </div>
            ))}
            
            {isAskingAI && (
              <div className="flex justify-start">
                <div className="max-w-[80%] bg-stone-100 text-stone-900 rounded-2xl rounded-tl-sm px-4 py-2">
                  <div className="flex items-start gap-2">
                    <Bot className="w-4 h-4 mt-0.5 flex-shrink-0 text-orange-500" />
                    <Loader2 className="w-4 h-4 animate-spin" />
                  </div>
                </div>
              </div>
            )}
          </CardContent>
          {/* 채팅 입력창 */}
          <div className="p-4 border-t border-stone-100">
            <div className="flex gap-2">
              <Input
                type="text"
                placeholder="AI에게 질문하기..."
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                className="flex-1 rounded-lg border-stone-200"
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && message.trim() && !isAskingAI) {
                    handleAskAI();
                  }
                }}
                disabled={isAskingAI}
              />
              <Button
                size="icon"
                className="rounded-lg bg-orange-500 hover:bg-orange-600 text-white"
                onClick={handleAskAI}
                disabled={!message.trim() || isAskingAI}
              >
                {isAskingAI ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Send className="w-4 h-4" />
                )}
              </Button>
            </div>
          </div>
        </Card>,
        document.body
      )}
    </>
  );
}
