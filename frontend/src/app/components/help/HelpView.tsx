import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Search, ChevronRight, MessageCircle, Mail, ExternalLink, ChevronDown } from 'lucide-react';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '../ui/accordion';

interface FAQItem {
  question: string;
  answer: string;
}

const faqCategories = [
  {
    title: '모임 관리',
    items: [
      {
        question: '새로운 모임은 어떻게 만드나요?',
        answer: '홈 화면에서 우측 하단의 "+" 버튼을 탭하거나, "모임 만들기" 버튼을 눌러 새로운 모임을 생성할 수 있습니다. 모임 이름, 설명, 태그 등을 입력하고 생성 버튼을 누르면 됩니다.',
      },
      {
        question: '모임에 새 멤버를 초대하려면?',
        answer: '모임 관리 > 멤버 관리에서 "초대 링크 생성" 버튼을 눌러 초대 링크를 만들 수 있습니다. 이 링크를 카카오톡 등으로 공유하면 상대방이 쉽게 가입할 수 있습니다.',
      },
      {
        question: '모임장 권한을 다른 사람에게 넘길 수 있나요?',
        answer: '모임 관리 > 멤버 관리에서 원하는 멤버를 선택한 후 "모임장 위임" 버튼을 눌러 권한을 이전할 수 있습니다. 위임 후에는 일반 멤버가 됩니다.',
      },
    ],
  },
  {
    title: '일정 및 투표',
    items: [
      {
        question: '일정 투표는 어떻게 만드나요?',
        answer: '모임 내 일정 탭에서 "투표 만들기" 버튼을 눌러 새로운 일정 투표를 생성할 수 있습니다. 후보 날짜/시간을 추가하고 마감일을 설정하면 됩니다.',
      },
      {
        question: '투표 결과는 언제 확정되나요?',
        answer: '마감일이 지나면 자동으로 가장 많은 표를 받은 일정이 확정됩니다. 또는 모임장이 수동으로 일정을 확정할 수도 있습니다.',
      },
    ],
  },
  {
    title: '회비 관리',
    items: [
      {
        question: '회비 정책은 어떻게 설정하나요?',
        answer: '모임 관리 > 회비 정책 관리에서 월 회비 금액, 납부 마감일, 알림 설정 등을 지정할 수 있습니다.',
      },
      {
        question: '정산 요청은 어떻게 하나요?',
        answer: '회비 탭에서 "정산 요청" 버튼을 눌러 지출 항목과 금액을 입력합니다. 영수증 사진을 첨부하면 더 투명하게 관리할 수 있습니다.',
      },
      {
        question: '회비 납부 확인은 어떻게 하나요?',
        answer: '회비 탭에서 전체 내역을 확인하거나, 관리 > 멤버 관리에서 각 멤버별 납부 상태를 확인할 수 있습니다.',
      },
    ],
  },
  {
    title: '계정 및 설정',
    items: [
      {
        question: '비밀번호를 잊어버렸어요',
        answer: '로그인 화면에서 "비밀번호 찾기"를 눌러 가입한 이메일로 재설정 링크를 받을 수 있습니다.',
      },
      {
        question: '알림이 오지 않아요',
        answer: '설정 > 알림 설정에서 푸시 알림이 켜져 있는지 확인해주세요. 또한 휴대폰의 앱 알림 권한도 확인이 필요합니다.',
      },
      {
        question: '회원 탈퇴는 어떻게 하나요?',
        answer: '프로필 > 프로필 수정 > 회원 탈퇴에서 탈퇴할 수 있습니다. 탈퇴 전 모든 모임에서 나가야 하며, 모임장인 경우 권한을 먼저 위임해야 합니다.',
      },
    ],
  },
];

export function HelpView() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');

  const filteredCategories = faqCategories.map(category => ({
    ...category,
    items: category.items.filter(
      item =>
        item.question.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.answer.toLowerCase().includes(searchQuery.toLowerCase())
    ),
  })).filter(category => category.items.length > 0);

  return (
    <div className="min-h-screen bg-stone-50">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center px-4 py-3">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigate(-1)}
            className="-ml-2"
          >
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="ml-2 text-lg font-semibold text-stone-800">도움말</h1>
        </div>

        {/* Search */}
        <div className="px-4 pb-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-stone-400" />
            <Input
              placeholder="궁금한 내용을 검색하세요"
              className="pl-10 h-11 bg-stone-50 border-stone-200 rounded-xl"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
        </div>
      </header>

      <div className="p-4 space-y-6">
        {/* FAQ */}
        {filteredCategories.length > 0 ? (
          filteredCategories.map(category => (
            <div key={category.title} className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
              <div className="px-4 py-3 bg-stone-50 border-b border-stone-100">
                <h2 className="font-semibold text-stone-900">{category.title}</h2>
              </div>
              <Accordion type="single" collapsible>
                {category.items.map((item, index) => (
                  <AccordionItem key={index} value={`item-${index}`} className="border-b border-stone-100 last:border-0">
                    <AccordionTrigger className="px-4 py-3 text-left hover:no-underline hover:bg-stone-50">
                      <span className="text-sm font-medium text-stone-900 pr-4">{item.question}</span>
                    </AccordionTrigger>
                    <AccordionContent className="px-4 pb-4">
                      <p className="text-sm text-stone-600 leading-relaxed">{item.answer}</p>
                    </AccordionContent>
                  </AccordionItem>
                ))}
              </Accordion>
            </div>
          ))
        ) : (
          <div className="text-center py-12">
            <p className="text-stone-500">검색 결과가 없습니다</p>
          </div>
        )}

        {/* Contact */}
        <div className="bg-white rounded-2xl border border-stone-100 overflow-hidden">
          <div className="px-4 py-3 bg-stone-50 border-b border-stone-100">
            <h2 className="font-semibold text-stone-900">문의하기</h2>
          </div>
          <div className="divide-y divide-stone-100">
            <button className="w-full flex items-center justify-between p-4 hover:bg-stone-50 transition-colors">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                  <MessageCircle className="w-5 h-5 text-green-600" />
                </div>
                <div className="text-left">
                  <p className="font-medium text-stone-900">카카오톡 문의</p>
                  <p className="text-xs text-stone-500">평일 10:00 - 18:00</p>
                </div>
              </div>
              <ExternalLink className="w-5 h-5 text-stone-300" />
            </button>
            <button className="w-full flex items-center justify-between p-4 hover:bg-stone-50 transition-colors">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                  <Mail className="w-5 h-5 text-blue-600" />
                </div>
                <div className="text-left">
                  <p className="font-medium text-stone-900">이메일 문의</p>
                  <p className="text-xs text-stone-500">support@example.com</p>
                </div>
              </div>
              <ChevronRight className="w-5 h-5 text-stone-300" />
            </button>
          </div>
        </div>

        {/* Version */}
        <p className="text-center text-xs text-stone-400 py-4">
          앱 버전 1.0.0
        </p>
      </div>
    </div>
  );
}



