// QA 체크리스트 2 (FE): 2MB 초과 파일은 alert 후 업로드 요청을 보내지 않는다
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mountWith, settle, fakeFile } from './helpers'
import AttachmentUploader from '@/components/attachments/AttachmentUploader.vue'
import { uploadAttachment } from '@/api/attachments'
import { ElMessageBox } from 'element-plus'

vi.mock('@/api/attachments', () => ({ uploadAttachment: vi.fn() }))
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessageBox: { ...actual.ElMessageBox, alert: vi.fn(() => Promise.resolve()) },
    ElMessage: { ...actual.ElMessage, error: vi.fn(), success: vi.fn() }
  }
})

async function selectFiles(wrapper, files) {
  const input = wrapper.find('[data-testid="attachment-input"]')
  Object.defineProperty(input.element, 'files', { value: files, configurable: true })
  await input.trigger('change')
  await settle()
}

describe('AttachmentUploader', () => {
  let wrapper
  beforeEach(() => vi.clearAllMocks())
  afterEach(() => wrapper?.unmount())

  it('2MB + 1 byte: alert, 업로드 미호출, rejected(TOO_LARGE)', async () => {
    wrapper = mountWith(AttachmentUploader, { props: { issueId: 10 } })
    await selectFiles(wrapper, [fakeFile('big.pdf', 2097153)])

    expect(ElMessageBox.alert).toHaveBeenCalledWith('파일 크기는 2MB를 초과할 수 없습니다.', '업로드 불가', { type: 'error' })
    expect(uploadAttachment).not.toHaveBeenCalled()
    expect(wrapper.emitted('rejected')).toEqual([['TOO_LARGE']])
    expect(wrapper.emitted('uploaded')).toBeUndefined()
  })

  it('정확히 2MB: 즉시 업로드 모드에서 업로드 후 uploaded emit', async () => {
    const attachment = { id: 5, originalFilename: 'ok.pdf' }
    uploadAttachment.mockResolvedValue(attachment)
    wrapper = mountWith(AttachmentUploader, { props: { issueId: 10 } })
    const file = fakeFile('ok.pdf', 2097152)
    await selectFiles(wrapper, [file])

    expect(ElMessageBox.alert).not.toHaveBeenCalled()
    expect(uploadAttachment).toHaveBeenCalledWith(10, file)
    expect(wrapper.emitted('uploaded')).toEqual([[attachment]])
  })

  it('대기열 모드(issueId 없음): 통과한 파일만 queued, 초과 파일은 제외', async () => {
    wrapper = mountWith(AttachmentUploader, { props: { issueId: null } })
    const ok = fakeFile('ok.txt', 1024)
    await selectFiles(wrapper, [fakeFile('big.zip', 3 * 1024 * 1024), ok])

    expect(uploadAttachment).not.toHaveBeenCalled()
    expect(wrapper.emitted('rejected')).toEqual([['TOO_LARGE']])
    expect(wrapper.emitted('queued')).toEqual([[[ok]]])
  })

  it('허용되지 않은 형식은 형식 안내 alert', async () => {
    wrapper = mountWith(AttachmentUploader, { props: { issueId: 10 } })
    await selectFiles(wrapper, [fakeFile('run.exe', 10)])
    expect(ElMessageBox.alert).toHaveBeenCalledWith('허용되지 않는 파일 형식입니다.', '업로드 불가', { type: 'error' })
    expect(uploadAttachment).not.toHaveBeenCalled()
  })

  it('서버 413 응답도 같은 2MB 문구로 alert', async () => {
    uploadAttachment.mockRejectedValue({ response: { status: 413, data: { code: 'FILE_TOO_LARGE' } } })
    wrapper = mountWith(AttachmentUploader, { props: { issueId: 10 } })
    await selectFiles(wrapper, [fakeFile('ok.pdf', 1000)])
    expect(ElMessageBox.alert).toHaveBeenCalledWith('파일 크기는 2MB를 초과할 수 없습니다.', '업로드 불가', { type: 'error' })
    expect(wrapper.emitted('uploaded')).toBeUndefined()
  })
})
