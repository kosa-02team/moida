import { useState } from 'react';
import { MessageCircle, X, Bot, User, Send } from 'lucide-react';
import { Button } from '../../ui/button';
import { Card, CardContent, CardHeader } from '../../ui/card';
import { Input } from '../../ui/input';

interface QAPair {
  question: string;
  answer: string;
}

const dummyQA: QAPair[] = [
  { question: "kosa 프로젝트 참여자들은 몇명이였어?", answer: "19명입니다" },
  { question: "1월에 간 횟집 장소는 어디였어?", answer: "태희네 횟집입니다." },
  { question: "누구랑 갔지?", answer: "김태희랑 갔습니다." }
];

export function QAChatWidget() {
  const [isExpanded, setIsExpanded] = useState(false);
  const [message, setMessage] = useState('');

  return (
    <div className="relative">
      {!isExpanded ? (
        // 접힌 상태: Q&A 버튼
        <Button
          onClick={() => setIsExpanded(true)}
          className="rounded-full h-14 w-14 shadow-lg bg-orange-500 hover:bg-orange-600 text-white p-0"
          aria-label="Q&A 열기"
        >
          <MessageCircle className="w-6 h-6" />
        </Button>
      ) : (
        // 펼친 상태: 채팅 UI
        <Card className="absolute bottom-16 left-0 w-[420px] h-[600px] shadow-xl border-stone-200 flex flex-col z-50">
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
            {dummyQA.map((qa, index) => (
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
                      <p className="text-sm">{qa.answer}</p>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </CardContent>
          {/* 채팅 입력창 */}
          <div className="p-4 border-t border-stone-100">
            <div className="flex gap-2">
              <Input
                type="text"
                placeholder="메시지를 입력하세요..."
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                className="flex-1 rounded-lg border-stone-200"
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && message.trim()) {
                    // 엔터키로 전송 (실제 로직은 나중에 구현)
                    setMessage('');
                  }
                }}
              />
              <Button
                size="icon"
                className="rounded-lg bg-orange-500 hover:bg-orange-600 text-white"
                onClick={() => {
                  if (message.trim()) {
                    // 메시지 전송 (실제 로직은 나중에 구현)
                    setMessage('');
                  }
                }}
                disabled={!message.trim()}
              >
                <Send className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
}
